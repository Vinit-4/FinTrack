package com.fintrack.entity;

/**
 * Transaction categories.
 * Stored in the database as text (see @Enumerated(EnumType.STRING) on the entity),
 * so adding a new value here is all that is needed to extend the list.
 */
public enum Category {
    FOOD,
    TRANSPORT,
    SHOPPING,
    ENTERTAINMENT,
    EDUCATION,
    BILLS,
    HEALTH,
    RENT,
    SALARY,
    INVESTMENT,
    OTHER
}
