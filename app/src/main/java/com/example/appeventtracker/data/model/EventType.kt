package com.example.appeventtracker.data.model


/** As per the requirement mentioned in the document
 * INSTALL: only one accepted installation event globally.
 * VISIT: only one accepted visit per session.
 * PURCHASE and ADD_TO_CART: repeatable events.
 */
enum class EventType {
    INSTALL,
    VISIT,
    PURCHASE,
    ADD_TO_CART
}