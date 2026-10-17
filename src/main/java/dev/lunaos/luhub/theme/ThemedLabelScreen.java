package dev.lunaos.luhub.theme;

/** Screens, die eine eigene Überschriften-Farbe brauchen (z.B. eingefärbte Shulkerkisten). */
public interface ThemedLabelScreen {
	/** @return ARGB-Farbe oder 0 für die normale Theme-Farbe */
	int luhub$labelColor();
}
