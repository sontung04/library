package com.personal.loan.domain.services;

enum Action {

    CREATE("created"),
    UPDATE("updated"),
    DELETE("deleted");

    private final String label;

    Action(String label) {
        this.label = label;
    }
    
    String label() {
        return this.label;
    }
}
