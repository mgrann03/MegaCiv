package net.bubbaland.gui;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JRootPane;
import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * A class that allows tabs to be dragged off of a frame.
 *
 * This class provides a drop location when a tab is dragged off a frame by having a window follow the cursor around
 * when not inside of a @TriviaFrame. When a tab is dropped into this, it passes the drop data along to a new
 * TriviaFrame.
 *
 * @author Walter Kolczynski
 *
 */
public class TearAwayTab extends JWindow {
	private static final long			serialVersionUID	= -2723420566227526365L;

	// A timer to poll the mouse location and move the window around
	private final Timer					mousePoller;

	private final GhostGlassPane		glassPane;

	private final BubbaDragDropTabFrame	sourceFrame;

	public TearAwayTab(final BubbaDragDropTabFrame sourceFrame) {
		this.sourceFrame = sourceFrame;
		this.glassPane = new GhostGlassPane();
		this.add(this.glassPane);
		// Create a timer to poll the mouse location and update the window location
		this.mousePoller = new Timer(50, new ActionListener() {
			private Point lastPoint = MouseInfo.getPointerInfo().getLocation();

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Point point = MouseInfo.getPointerInfo().getLocation();
				if (!point.equals(this.lastPoint)) {
					TearAwayTab.this.center(point);
				}
				this.lastPoint = point;
			}
		});
		// Make this a valid drop target
		new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, new EasyDropTarget(), true);
		// Make frame transparent
		this.setBackground(new Color(0, 255, 0, 0));
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (ge.getDefaultScreenDevice().isWindowTranslucencySupported(TRANSLUCENT)) {
			this.setOpacity(0.7f);
		} else {
			this.setOpacity(1.0f);
		}
		// Don't display this until needed
		this.setVisible(false);
	}

	/**
	 * Display this window and attach it to the mouse pointer.
	 *
	 * @param tabbedPane
	 *            The tabbed pane to attach to
	 * @param tabIndex
	 *            The position where this should be inserted
	 */
	public void attach(final BubbaDnDTabbedPane tabbedPane, final int tabIndex) {
		if (this.isVisible()) {
			return;
		}
		// Get image of tab
		final Rectangle rect = tabbedPane.getBoundsAt(tabIndex);
		BufferedImage tabImage =
				new BufferedImage(tabbedPane.getWidth(), tabbedPane.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics g = tabImage.getGraphics();
		tabbedPane.paint(g);
		tabImage = tabImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
		// Get image of panel
		final Component panel = tabbedPane.getComponentAt(tabIndex);
		final BufferedImage panelImage =
				new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics panelGraphics = panelImage.getGraphics();
		panel.paint(panelGraphics);
		final int combinedHeight = tabImage.getHeight() + panelImage.getHeight();
		// Combine images into single image
		final BufferedImage combinedImage =
				new BufferedImage(panelImage.getWidth(), combinedHeight, BufferedImage.TYPE_INT_ARGB);
		combinedImage.createGraphics().drawImage(tabImage, 0, 0, null);
		combinedImage.createGraphics().drawImage(panelImage, 0, tabImage.getHeight(), null);
		// Set image of pane
		this.glassPane.setImage(combinedImage);
		// Set size & location and start polling mouse
		this.setSize(panel.getSize());
		this.mousePoller.start();
		this.setVisible(true);
	}

	/**
	 * Stop displaying this window.
	 */
	public void detach() {
		this.mousePoller.stop();
		this.setVisible(false);
	}

	/**
	 * Move the window.
	 *
	 * @param location
	 *            The new window location
	 */
	private void center(final Point location) {
		final Point offsetLocation = location;
		offsetLocation.setLocation(location.x - 10, location.y - 10);
		TearAwayTab.this.setLocation(offsetLocation);
		for (final BubbaDnDTabbedPane pane : BubbaDnDTabbedPane.getTabbedPanes()) {
			final JRootPane root = pane.getRootPane();
			final Rectangle bounds = root.getBounds();
			bounds.setLocation(root.getLocationOnScreen());
			if (bounds.contains(location)) {
				this.setVisible(false);
				return;
			}
		}
		this.setVisible(true);
	}

	/**
	 * A drop target to handle creation of a new frame when a tab is dropped.
	 *
	 * @author Walter Kolczynski
	 *
	 */
	private class EasyDropTarget implements DropTargetListener {

		@Override
		public void dragEnter(final DropTargetDragEvent dtde) {
			dtde.acceptDrag(dtde.getDropAction());
		}

		@Override
		public void dragExit(final DropTargetEvent dte) {}

		@Override
		public void dragOver(final DropTargetDragEvent dtde) {}

		@Override
		public void drop(final DropTargetDropEvent a_event) {
			TearAwayTab.this.detach();
			final BubbaDragDropTabFrame newFrame = TearAwayTab.this.sourceFrame.deriveNewFrame();
			newFrame.tabbedPane.convertTab(newFrame.tabbedPane.getTabTransferData(a_event),
					newFrame.tabbedPane.getTargetTabIndex(a_event.getLocation()));
			newFrame.tabbedPane.setSelectedIndex(0);
			newFrame.pack();
			newFrame.setLocation(TearAwayTab.this.getLocation());
			newFrame.setCursor(null);
			a_event.dropComplete(true);
		}

		@Override
		public void dropActionChanged(final DropTargetDragEvent dtde) {}
	}
}