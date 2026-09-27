# FinTrack ML extension (optional)

This folder is **not** part of the running application. The Spring Boot backend
and the React frontend work fully without it. It exists to show one small,
explainable machine-learning idea on top of the data FinTrack already collects.

## What it does

It reads your past monthly expense totals and fits a straight line through them
(ordinary least squares, also called simple linear regression). The value of that
line one month into the future is the prediction.

It also reports **R²**, which says how well a straight line actually describes
your spending. If R² is low, the script says so instead of pretending the number
is reliable. With fewer than three months of history it refuses to predict at all.

## Run it

```bash
cd ml-service
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Option 1 - from the bundled sample file
python predict.py --csv sample-history.csv

# Option 2 - from your own FinTrack database
export DATABASE_URL="postgresql://postgres:yourpassword@localhost:5432/fintrack"
python predict.py --user-id 1
```

Example output:

```
History used     : 6 months (2025-10 to 2026-03)
Average so far   : 21266.67
Trend per month  : +892.86
Fit quality (R2) : 0.628
Next month        : 24148.57
```

## Why linear regression, and not something bigger

Six to twelve data points is far too little for a neural network: it would
memorise the noise. A line has two parameters, needs almost no data, and you can
explain it in one sentence in an interview — "spending changes by about ₹890 per
month, so next month is this month plus ₹890."

## How to plug it into FinTrack later

Wrap the same function in a FastAPI service and let Spring Boot call it over HTTP:

```python
# app.py
from fastapi import FastAPI, HTTPException
from predict import read_from_database, predict_next_month, MINIMUM_MONTHS
import os

app = FastAPI()

@app.get("/predict/{user_id}")
def predict(user_id: int):
    history = read_from_database(user_id, os.environ["DATABASE_URL"])
    if len(history) < MINIMUM_MONTHS:
        raise HTTPException(status_code=422, detail="Not enough history to predict")
    return predict_next_month(history)
```

Run it with `uvicorn app:app --port 8000`, then on the Java side add a small
`SpendingForecastService` that uses Spring's `RestClient` to call
`http://localhost:8000/predict/{userId}`, wrapped in a try/catch so the dashboard
still renders if the Python service is down. Sharing the database like this is
fine for a learning project; a production system would have the ML service ask
the Java API for the data instead of reading its tables directly.
