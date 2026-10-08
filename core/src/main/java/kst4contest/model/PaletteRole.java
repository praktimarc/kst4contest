package kst4contest.model;

/**
 * A colour the operator may set, and the stylesheet declaration it corresponds to.
 *
 * <p>In core and therefore free of any toolkit: a role is a name and three strings, and the
 * colour itself travels as text. Turning text into a drawable colour is the user
 * interface's job.</p>
 *
 * <p>A declaration is a selector and a property together. Three of the six roles are stated
 * outside {@code .root} by the shipped sheets, and {@code -fx-background-color} alone appears
 * a dozen times in one of them -- on scroll bars, buttons and toggle buttons. Without the
 * selector, half the roles would never be found and the rest would be found in the wrong
 * places.</p>
 *
 * <p>All three strings are part of a file format. The selector and the property are what a
 * hand-edited stylesheet writes; {@link #key()} is what the stored overrides are written
 * under. Renaming any of them silently drops a colour an operator chose.</p>
 */
public enum PaletteRole {

	/** The control surface; the window surface is normally a {@code derive} of this. */
	SURFACE(".root", "-fx-base", "surface"),

	/** The window's own area. Neither shipped sheet states it, so both derive it. */
	WINDOW_SURFACE(".root", "-fx-background", "windowSurface"),

	/** Inside a text field or a list. */
	FIELD_INTERIOR(".root", "-fx-control-inner-background", "fieldInterior"),

	/** Label text. Stated on {@code .label} and not on {@code .root}. */
	TEXT(".label", "-fx-text-fill", "text"),

	ACCENT(".root", "-fx-accent", "accent"),

	/** The separator's line, whose selector is what tells it from a scroll bar. */
	SEPARATOR(".separator *.line", "-fx-background-color", "separator");

	private final String selector;
	private final String cssProperty;
	private final String key;

	PaletteRole(final String selector, final String cssProperty, final String key) {
		this.selector = selector;
		this.cssProperty = cssProperty;
		this.key = key;
	}

	/**
	 * Returns the stylesheet selector this role is stated on.
	 *
	 * @return selector
	 */
	public String selector() {
		return selector;
	}

	/**
	 * Returns the stylesheet property this role is read from.
	 *
	 * @return CSS property name
	 */
	public String cssProperty() {
		return cssProperty;
	}

	/**
	 * Returns the name this role is stored under in the preferences.
	 *
	 * @return storage key
	 */
	public String key() {
		return key;
	}

	/**
	 * Returns the role a stylesheet declaration belongs to.
	 *
	 * @param selector a selector from a stylesheet, may be null
	 * @param cssProperty a property name from a stylesheet, may be null
	 * @return the role, or null for the hundreds of declarations that are not ours
	 */
	public static PaletteRole of(final String selector, final String cssProperty) {

		if (selector == null || cssProperty == null) {
			return null;
		}

		for (PaletteRole role : values()) {
			if (role.selector.equals(selector) && role.cssProperty.equals(cssProperty)) {
				return role;
			}
		}

		return null;
	}
}
