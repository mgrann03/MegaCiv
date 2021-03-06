package net.bubbaland.megaciv.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.apache.commons.lang3.text.WordUtils;

import net.bubbaland.gui.BubbaDragDropTabFrame;
import net.bubbaland.megaciv.game.Civilization;

public class MegaCivFrame extends BubbaDragDropTabFrame implements ActionListener {

	private static final long	serialVersionUID	= -8995125745966985308L;

	private final GuiController	controller;
	private final GuiClient		client;

	protected MegaCivFrame(final GuiClient client, final GuiController controller) {
		super(controller);
		this.setSaveTabs(false);
		this.client = client;
		this.controller = controller;
		this.initTabInfoHash();

		this.tabbedPane.setFont(this.tabbedPane.getFont()
				.deriveFont(Float.parseFloat(this.controller.getProperties().getProperty("Tab.FontSize", "12f"))));

		// Create Menu
		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		final JMenu gameMenu = new JMenu("Game");
		gameMenu.setMnemonic(KeyEvent.VK_G);
		menuBar.add(gameMenu);

		JMenuItem menuItem = new JMenuItem("New Game");
		menuItem.setMnemonic(KeyEvent.VK_N);
		menuItem.setActionCommand("New Game");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		menuItem = new JMenuItem("Save Game");
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.setActionCommand("Save Game");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		menuItem = new JMenuItem("Load Save");
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.setActionCommand("Load Save");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		gameMenu.addSeparator();

		menuItem = new JMenuItem("Retire a Player");
		menuItem.setActionCommand("Retire");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		gameMenu.addSeparator();

		menuItem = new JMenuItem("Change User Name");
		menuItem.setActionCommand("Change Name");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		menuItem = new JMenuItem("Load Defaults");
		menuItem.setActionCommand("Load Defaults");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);

		menuItem = new JMenuItem("Quit");
		menuItem.setActionCommand("Quit");
		menuItem.addActionListener(this);
		gameMenu.add(menuItem);
	}

	@Override
	public ArrayList<String> getTabNames() {
		final ArrayList<String> tabNames = super.getTabNames();
		final ArrayList<Civilization.Name> civNames =
				( this.client.getGame() == null ) ? new ArrayList<Civilization.Name>() : this.client.getGame()
						.getCivilizationNames();
		for (final Iterator<String> iterator = tabNames.iterator(); iterator.hasNext();) {
			final String tabName = iterator.next();
			if (Civilization.Name.contains(tabName)
					&& !civNames.contains(Civilization.Name.valueOf(tabName.toUpperCase()))) {
				iterator.remove();
			}
		}
		return tabNames;
	}

	@Override
	public MegaCivFrame deriveNewFrame() {
		return new MegaCivFrame(this.client, this.controller);
	}

	@Override
	protected void initTabInfoHash() {
		super.initTabInfoHash();
		this.tabInformationHash.put("AST",
				new TabInformation("Panel showing AST", AstTabPanel.class,
						new Class<?>[] { GuiClient.class, GuiController.class, MegaCivFrame.class },
						new Object[] { this.client, this.controller, this }));
		this.tabInformationHash.put("Trade",
				new TabInformation("Tab listing trade cards", TradeCardPanel.class,
						new Class<?>[] { GuiClient.class, GuiController.class, MegaCivFrame.class },
						new Object[] { this.client, this.controller, this }));
		for (final Civilization.Name name : EnumSet.allOf(Civilization.Name.class)) {
			this.tabInformationHash.put(WordUtils.capitalizeFully(name.toString()),
					new TabInformation(name.toString() + " Information", CivInfoPanel.class,
							new Class<?>[] { GuiClient.class, GuiController.class, MegaCivFrame.class,
									Civilization.Name.class },
							new Object[] { this.client, this.controller, this, name }));
		}
	}

	@Override
	public void updateGui() {
		super.updateGui();
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		final String command = event.getActionCommand();
		switch (command) {
			case "New Game":
				new NewGameDialog(this.client, this.controller);
				break;
			case "Save Game": {
				final JFileChooser chooser = new JFileChooser();
				final int returnVal = chooser.showOpenDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					this.client.saveGame(chooser.getSelectedFile());
				}
				break;
			}
			case "Load Save": {
				final JFileChooser chooser = new JFileChooser();
				final int returnVal = chooser.showOpenDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					this.client.loadGame(chooser.getSelectedFile());
				}
				break;
			}
			case "Retire":
				new RetireDialog(this.client, this.controller);
				break;
			case "Load Defaults":
				this.controller.loadDefaults();
				break;
			case "Quit":
				this.controller.endProgram();
				break;
			case "Change Name":
				new UserDialog(this.controller, this.client);
				break;
			default:
				this.setStatusBarMessage(
						"Unknown action command " + command + "received by " + this.getClass().getSimpleName());
		}
	}

	public MegaCivFrame copy() {
		return new MegaCivFrame(this.client, this.controller);
	}
}
