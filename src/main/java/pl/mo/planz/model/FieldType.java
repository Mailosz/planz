package pl.mo.planz.model;

public enum FieldType {
    /**
     * Pole tekstowe
     */
    TEXT,
    /**
     * Kod HTML
     */
    CODE, 
    /**
     * Wartość z listy
     */
    LIST, 
    /**
     * Pole automatyczne
     */
    AUTO, 
    /**
     * Wartość kopiowana z innego pola
     */
    COPY, 
    /**
     * Pole skryptowe
     */
    COMP, 
    /**
     * Pole niewidoczne
     */
    HIDDEN, 
    /**
     * Część dokumentu zawierająca wewnętrzne pola
     */
    PART
}
