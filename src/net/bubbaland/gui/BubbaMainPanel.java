package net.bubbaland.gui;

import java.awt.GridBagLayout;

/**
 * Super-class for most of the panels in the trivia GUI.
 *
 * Provides methods for automatically making labels and text areas that fill their space by enclosing them in panels
 *
 */
public abstract class BubbaMainPanel extends BubbaPanel {

	private static final long	serialVersionUID	= -5381727804575779591L;

	protected BubbaFrame		frame;

	/**
	 * Instantiates a new Trivia Panel
	 *
	 * @param controller
	 *            TODO
	 * @param frame
	 *            TODO
	 */
	public BubbaMainPanel(final BubbaGuiController controller, final BubbaFrame frame) {
		super(controller, new GridBagLayout());
		this.frame = frame;
	}

	public void changeFrame(final BubbaFrame newFrame) {
		this.frame = newFrame;
	}

	/**
	 * Requires all sub-classes to have a method that updates their contents.
	 */
	public abstract void updateGui();


	protected abstract void loadProperties();


}
