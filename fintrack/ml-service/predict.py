"""
FinTrack - optional spending predictor.

Predicts next month's total expense from a user's past monthly expense totals
using ordinary least squares (a straight line fitted through the points).

It is deliberately a separate script: the Spring Boot application does not call
it and works perfectly without it.

Two ways to give it data
------------------------
1. From a CSV file with the columns month,total_expense:

       python predict.py --csv sample-history.csv

2. Straight from the FinTrack database, for one user:

       export DATABASE_URL="postgresql://postgres:password@localhost:5432/fintrack"
       python predict.py --user-id 1

Install first:  pip install -r requirements.txt
"""

import argparse
import csv
import os
import sys

import numpy as np

# At least this many months of history before a prediction means anything.
MINIMUM_MONTHS = 3


def read_from_csv(path):
    """Returns [(month_label, total_expense), ...] ordered oldest first."""
    rows = []
    with open(path, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            rows.append((row["month"], float(row["total_expense"])))
    rows.sort(key=lambda item: item[0])
    return rows


def read_from_database(user_id, database_url):
    """Groups the user's EXPENSE rows into one total per calendar month."""
    import psycopg2  # imported here so the CSV mode needs no database driver

    query = """
        SELECT to_char(date_trunc('month', transaction_date), 'YYYY-MM') AS month,
               SUM(amount) AS total_expense
        FROM transactions
        WHERE user_id = %s AND type = 'EXPENSE'
        GROUP BY 1
        ORDER BY 1
    """

    with psycopg2.connect(database_url) as connection:
        with connection.cursor() as cursor:
            cursor.execute(query, (user_id,))
            return [(month, float(total)) for month, total in cursor.fetchall()]


def predict_next_month(history):
    """
    Fits total_expense = slope * month_index + intercept and evaluates it
    one month past the end of the history.
    """
    y = np.array([total for _, total in history], dtype=float)
    x = np.arange(len(y), dtype=float)

    slope, intercept = np.polyfit(x, y, 1)
    predicted = slope * len(y) + intercept

    # A straight line can point below zero; spending cannot.
    predicted = max(predicted, 0.0)

    # R^2: how much of the variation the line actually explains (1.0 = perfect).
    fitted = slope * x + intercept
    residual = float(np.sum((y - fitted) ** 2))
    total_variance = float(np.sum((y - np.mean(y)) ** 2))
    r_squared = 1.0 - residual / total_variance if total_variance > 0 else 0.0

    return {
        "predicted_expense": round(float(predicted), 2),
        "monthly_change": round(float(slope), 2),
        "r_squared": round(r_squared, 3),
        "months_used": len(y),
        "average_of_history": round(float(np.mean(y)), 2),
    }


def main():
    parser = argparse.ArgumentParser(description="Predict next month's spending")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--csv", help="CSV file with columns month,total_expense")
    source.add_argument("--user-id", type=int, help="FinTrack user id (needs DATABASE_URL)")
    args = parser.parse_args()

    if args.csv:
        history = read_from_csv(args.csv)
    else:
        database_url = os.environ.get("DATABASE_URL")
        if not database_url:
            print("DATABASE_URL is not set. Export it before using --user-id.", file=sys.stderr)
            return 1
        history = read_from_database(args.user_id, database_url)

    if len(history) < MINIMUM_MONTHS:
        # Honest refusal instead of a confident-looking number from two points.
        print(
            f"Not enough history: {len(history)} month(s) found, "
            f"{MINIMUM_MONTHS} needed. Keep recording transactions and try again."
        )
        return 0

    result = predict_next_month(history)

    print(f"History used     : {result['months_used']} months "
          f"({history[0][0]} to {history[-1][0]})")
    print(f"Average so far   : {result['average_of_history']:.2f}")
    print(f"Trend per month  : {result['monthly_change']:+.2f}")
    print(f"Fit quality (R2) : {result['r_squared']:.3f}")
    print(f"Next month        : {result['predicted_expense']:.2f}")

    if result["r_squared"] < 0.3:
        print("\nNote: the months vary a lot, so this trend line is a weak guide. "
              "Treat the average as the safer estimate.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
