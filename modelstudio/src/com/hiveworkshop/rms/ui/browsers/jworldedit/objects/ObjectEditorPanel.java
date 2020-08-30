package com.hiveworkshop.rms.ui.browsers.jworldedit.objects;

import com.hiveworkshop.rms.filesystem.GameDataFileSystem;
import com.hiveworkshop.rms.filesystem.sources.CompoundDataSource;
import com.hiveworkshop.rms.parsers.slk.DataTable;
import com.hiveworkshop.rms.parsers.slk.GameObject;
import com.hiveworkshop.rms.parsers.slk.StandardObjectData;
import com.hiveworkshop.rms.ui.browsers.unit.UnitOptionPanel;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.datamodel.MutableObjectData;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.datamodel.MutableObjectData.MutableGameObject;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.datamodel.MutableObjectData.WorldEditorDataType;
import com.hiveworkshop.rms.parsers.w3o.WTSFile;
import com.hiveworkshop.rms.util.War3ID;
import com.hiveworkshop.rms.parsers.w3o.War3ObjectDataChangeset;
import com.hiveworkshop.rms.parsers.blp.BLPHandler;
import com.hiveworkshop.rms.ui.gui.modeledit.util.TransferActionListener;
import com.hiveworkshop.rms.ui.icons.IconUtils;
import com.hiveworkshop.rms.ui.browsers.jworldedit.AbstractWorldEditorPanel;
import com.hiveworkshop.rms.ui.browsers.jworldedit.WEString;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.better.fields.builders.*;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.better.fields.factory.BasicSingleFieldFactory;
import de.wc3data.stream.BlizzardDataInputStream;
import de.wc3data.stream.BlizzardDataOutputStream;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ObjectEditorPanel extends AbstractWorldEditorPanel {
	private static final War3ID UNIT_NAME = War3ID.fromString("unam");

	private final List<UnitEditorPanel> editors = new ArrayList<>();
	private JButton createNewButton;
	private JButton pasteButton;
	private JButton copyButton;
	private final JTabbedPane tabbedPane;

	private MutableObjectData unitData;

	private final JFileChooser jFileChooser;

	public ObjectEditorPanel() {
		tabbedPane = new JTabbedPane() {
			@Override
			public void addTab(final String title, final Icon icon, final Component component) {
				super.addTab(title, icon, component);
				editors.add((UnitEditorPanel) component);
			}
		};
		final DataTable worldEditorData = DataTable.getWorldEditorData();
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_UNITS"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewUnit"), createUnitEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_ITEMS"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewItem"), createItemEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_DESTRUCTABLES"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewDest"), createDestructibleEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_DOODADS"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewDood"), createDoodadEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_ABILITIES"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewAbil"), createAbilityEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_BUFFS"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewBuff"), createAbilityBuffEditor());
		tabbedPane.addTab(WEString.getString("WESTRING_OBJTAB_UPGRADES"),
				getIcon(worldEditorData, "ToolBarIcon_OE_NewUpgr"), createUpgradeEditor());
		tabbedPane.addTab("Terrain", getIcon(worldEditorData, "ToolBarIcon_Module_Terrain"), createUpgradeEditor());
		tabbedPane.addTab("Lighting Effects",
				new ImageIcon(IconUtils.worldEditStyleIcon(
						BLPHandler.get().getGameTex("ReplaceableTextures\\CommandButtons\\BTNChainLightning.blp"))),
				createUpgradeEditor());
		tabbedPane.addTab("Weather",
				new ImageIcon(IconUtils.worldEditStyleIcon(
						BLPHandler.get().getGameTex("ReplaceableTextures\\CommandButtons\\BTNMonsoon.blp"))),
				createUpgradeEditor());
		tabbedPane.addTab("Soundsets", getIcon(worldEditorData, "ToolBarIcon_Module_Sound"), createUpgradeEditor());

		final JToolBar toolBar = createToolbar(worldEditorData);
		toolBar.setFloatable(false);

		setLayout(new BorderLayout());

		add(toolBar, BorderLayout.BEFORE_FIRST_LINE);
		add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addChangeListener(e -> {
            final UnitEditorPanel selectedEditorPanel = (UnitEditorPanel) tabbedPane.getSelectedComponent();
            final EditorTabCustomToolbarButtonData editorTabCustomToolbarButtonData = selectedEditorPanel
                    .getEditorTabCustomToolbarButtonData();
            createNewButton.setIcon(getIcon(worldEditorData, editorTabCustomToolbarButtonData.getIconKey()));
            createNewButton.setToolTipText(
                    WEString.getString(editorTabCustomToolbarButtonData.getNewCustomObject()).replace("&", ""));
            copyButton.setToolTipText(
                    WEString.getString(editorTabCustomToolbarButtonData.getCopyObject()).replace("&", ""));
            pasteButton.setToolTipText(
                    WEString.getString(editorTabCustomToolbarButtonData.getPasteObject()).replace("&", ""));
        });
		jFileChooser = new JFileChooser(new File(System.getProperty("user.home") + "/Documents/Warcraft III/Maps"));
	}

	private JToolBar createToolbar(final DataTable worldEditorData) {
		final JToolBar toolBar = new JToolBar();
		makeButton(worldEditorData, toolBar, "newMap", "ToolBarIcon_New", "WESTRING_TOOLBAR_NEW");
		final JButton openButton = makeButton(worldEditorData, toolBar, "openMap", "ToolBarIcon_Open",
				"WESTRING_TOOLBAR_OPEN");
		openButton.addActionListener(e -> openSpecificTabData());
		final JButton saveButton = makeButton(worldEditorData, toolBar, "saveMap", "ToolBarIcon_Save",
				"WESTRING_TOOLBAR_SAVE");
		saveButton.addActionListener(e -> saveSpecificTabData());
		toolBar.add(Box.createHorizontalStrut(8));
		final TransferActionListener transferActionListener = new TransferActionListener();
		copyButton = makeButton(worldEditorData, toolBar, "copy", "ToolBarIcon_Copy", "WESTRING_MENU_OE_UNIT_COPY");
		copyButton.addActionListener(transferActionListener);
		copyButton.setActionCommand((String) TransferHandler.getCopyAction().getValue(Action.NAME));
		pasteButton = makeButton(worldEditorData, toolBar, "paste", "ToolBarIcon_Paste", "WESTRING_MENU_OE_UNIT_PASTE");
		pasteButton.addActionListener(transferActionListener);
		pasteButton.setActionCommand((String) TransferHandler.getPasteAction().getValue(Action.NAME));
		toolBar.add(Box.createHorizontalStrut(8));
		createNewButton = makeButton(worldEditorData, toolBar, "createNew", "ToolBarIcon_OE_NewUnit",
				"WESTRING_MENU_OE_UNIT_NEW");
		createNewButton.addActionListener(e -> {
            final UnitEditorPanel selectedEditorPanel = (UnitEditorPanel) tabbedPane.getSelectedComponent();
            selectedEditorPanel.runCustomUnitPopup();
        });
		toolBar.add(Box.createHorizontalStrut(8));
		makeButton(worldEditorData, toolBar, "terrainEditor", "ToolBarIcon_Module_Terrain",
				"WESTRING_MENU_MODULE_TERRAIN");
		makeButton(worldEditorData, toolBar, "scriptEditor", "ToolBarIcon_Module_Script",
				"WESTRING_MENU_MODULE_SCRIPTS");
		makeButton(worldEditorData, toolBar, "soundEditor", "ToolBarIcon_Module_Sound", "WESTRING_MENU_MODULE_SOUND");
		// final JButton objectEditorButton = makeButton(worldEditorData, toolBar,
		// "objectEditor",
		// "ToolBarIcon_Module_ObjectEditor", "WESTRING_MENU_OBJECTEDITOR");
		final JToggleButton objectEditorButton = new JToggleButton(
				getIcon(worldEditorData, "ToolBarIcon_Module_ObjectEditor"));
		objectEditorButton.setToolTipText(WEString.getString("WESTRING_MENU_OBJECTEDITOR").replace("&", ""));
		objectEditorButton.setPreferredSize(new Dimension(24, 24));
		objectEditorButton.setMargin(new Insets(1, 1, 1, 1));
		objectEditorButton.setSelected(true);
		objectEditorButton.setEnabled(false);
		objectEditorButton.setDisabledIcon(objectEditorButton.getIcon());
		toolBar.add(objectEditorButton);
		makeButton(worldEditorData, toolBar, "campaignEditor", "ToolBarIcon_Module_Campaign",
				"WESTRING_MENU_MODULE_CAMPAIGN");
		makeButton(worldEditorData, toolBar, "aiEditor", "ToolBarIcon_Module_AIEditor", "WESTRING_MENU_MODULE_AI");
		makeButton(worldEditorData, toolBar, "objectEditor", "ToolBarIcon_Module_ObjectManager",
				"WESTRING_MENU_OBJECTMANAGER");
		final String legacyImportManagerIcon = worldEditorData.get("WorldEditArt")
				.getField("ToolBarIcon_Module_ImportManager");
		String importManagerIconPath = "ToolBarIcon_Module_ImportManager";
		String importManagerMenuName = "WESTRING_MENU_IMPORTMANAGER";
		if ((legacyImportManagerIcon == null) || "".equals(legacyImportManagerIcon)) {
			importManagerIconPath = "ToolBarIcon_Module_AssetManager";
			importManagerMenuName = "WESTRING_MENU_ASSETMANAGER";
		}
		makeButton(worldEditorData, toolBar, "importEditor", importManagerIconPath, importManagerMenuName);
		toolBar.add(Box.createHorizontalStrut(8));
		makeButton(worldEditorData, toolBar, "testMap",
				new ImageIcon(IconUtils.worldEditStyleIcon(getIcon(worldEditorData, "ToolBarIcon_TestMap").getImage())),
				"WESTRING_TOOLBAR_TESTMAP").addActionListener(e -> ObjectEditorFrame.main(new String[]{}));
		return toolBar;
	}

	public void loadHotkeys() {
		final JRootPane root = getRootPane();
		getRootPane().getActionMap().put("displayAsRawData", new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				for (final UnitEditorPanel editor : editors) {
					editor.toggleDisplayAsRawData();
				}
			}
		});
		getRootPane().getActionMap().put("searchUnits", new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final int selectedIndex = tabbedPane.getSelectedIndex();
				final UnitEditorPanel unitEditorPanel = editors.get(selectedIndex);
				unitEditorPanel.doSearchForUnit();
			}
		});
		getRootPane().getActionMap().put("searchFindNextUnit", new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final int selectedIndex = tabbedPane.getSelectedIndex();
				final UnitEditorPanel unitEditorPanel = editors.get(selectedIndex);
				unitEditorPanel.doSearchFindNextUnit();
			}
		});
		root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control D"),
				"displayAsRawData");
		root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control F"),
				"searchUnits");
		root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control G"),
				"searchFindNextUnit");
		for (final UnitEditorPanel editor : editors) {
			editor.loadHotkeys();
		}
	}

	private UnitEditorPanel createUnitEditor() {
		final DataTable standardUnitMeta = StandardObjectData.getStandardUnitMeta();
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('u');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3u")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3u")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		unitData = new MutableObjectData(WorldEditorDataType.UNITS, StandardObjectData.getStandardUnits(),
				standardUnitMeta, unitDataChangeset);

		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(unitData, standardUnitMeta, new UnitFieldBuilder(),
				new UnitTabTreeBrowserBuilder(), WorldEditorDataType.UNITS,
				new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_UNIT_NEW", "ToolBarIcon_OE_NewUnit",
						"WESTRING_MENU_OE_UNIT_COPY", "WESTRING_MENU_OE_UNIT_PASTE"),
				new NewCustomUnitDialogRunner(unitData));
		return unitEditorPanel;
	}

	private UnitEditorPanel createItemEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('t');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3t")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3t")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final DataTable standardUnitMeta = StandardObjectData.getStandardUnitMeta();
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.ITEM, StandardObjectData.getStandardItems(), standardUnitMeta,
						unitDataChangeset),
				standardUnitMeta, new ItemFieldBuilder(), new ItemTabTreeBrowserBuilder(), WorldEditorDataType.ITEM,
				new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_ITEM_NEW", "ToolBarIcon_OE_NewItem",
						"WESTRING_MENU_OE_ITEM_COPY", "WESTRING_MENU_OE_ITEM_PASTE"),
                () -> {
                    // TODO Auto-generated method stub
                });
		return unitEditorPanel;
	}

	private UnitEditorPanel createDestructibleEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('b');
		final DataTable standardUnitMeta = StandardObjectData.getStandardDestructableMeta();
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3b")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3b")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.DESTRUCTIBLES, StandardObjectData.getStandardDestructables(),
						standardUnitMeta, unitDataChangeset),
				standardUnitMeta,
				new BasicEditorFieldBuilder(BasicSingleFieldFactory.INSTANCE, WorldEditorDataType.DESTRUCTIBLES),
				new DestructableTabTreeBrowserBuilder(), WorldEditorDataType.DESTRUCTIBLES,
				new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_DEST_NEW", "ToolBarIcon_OE_NewDest",
						"WESTRING_MENU_OE_DEST_COPY", "WESTRING_MENU_OE_DEST_PASTE"),
                () -> {
                    // TODO Auto-generated method stub

                });
		return unitEditorPanel;
	}

	private UnitEditorPanel createDoodadEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('d');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3d")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3d")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final DataTable standardUnitMeta = StandardObjectData.getStandardDoodadMeta();
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.DOODADS, StandardObjectData.getStandardDoodads(),
						standardUnitMeta, unitDataChangeset),
				standardUnitMeta, new DoodadFieldBuilder(), new DoodadTabTreeBrowserBuilder(),
				WorldEditorDataType.DOODADS, new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_DOOD_NEW",
				"ToolBarIcon_OE_NewDood", "WESTRING_MENU_OE_DOOD_COPY", "WESTRING_MENU_OE_DOOD_PASTE"),
                () -> {
                    // TODO Auto-generated method stub

                });
		return unitEditorPanel;
	}

	private JComponent createAbilityEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('a');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3a")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3a")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final DataTable standardUnitMeta = StandardObjectData.getStandardAbilityMeta();
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.ABILITIES, StandardObjectData.getStandardAbilities(),
						standardUnitMeta, unitDataChangeset),
				standardUnitMeta, new AbilityFieldBuilder(), new AbilityTabTreeBrowserBuilder(),
				WorldEditorDataType.ABILITIES, new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_ABIL_NEW",
				"ToolBarIcon_OE_NewAbil", "WESTRING_MENU_OE_ABIL_COPY", "WESTRING_MENU_OE_ABIL_PASTE"),
                () -> {
                    // TODO Auto-generated method stub

                });
		return unitEditorPanel;
	}

	private UnitEditorPanel createAbilityBuffEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('f');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3h")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3h")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final DataTable standardUnitMeta = StandardObjectData.getStandardAbilityBuffMeta();
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.BUFFS_EFFECTS, StandardObjectData.getStandardAbilityBuffs(),
						standardUnitMeta, unitDataChangeset),
				standardUnitMeta,
				new BasicEditorFieldBuilder(BasicSingleFieldFactory.INSTANCE, WorldEditorDataType.BUFFS_EFFECTS),
				new BuffTabTreeBrowserBuilder(), WorldEditorDataType.BUFFS_EFFECTS,
				new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_BUFF_NEW", "ToolBarIcon_OE_NewBuff",
						"WESTRING_MENU_OE_BUFF_COPY", "WESTRING_MENU_OE_BUFF_PASTE"),
                () -> {
                    // TODO Auto-generated method stub

                });
		return unitEditorPanel;
	}

	private UnitEditorPanel createUpgradeEditor() {
		final War3ObjectDataChangeset unitDataChangeset = new War3ObjectDataChangeset('g');
		try {
			final CompoundDataSource gameDataFileSystem = GameDataFileSystem.getDefault();
			if (gameDataFileSystem.has("war3map.w3q")) {
				unitDataChangeset.load(new BlizzardDataInputStream(gameDataFileSystem.getResourceAsStream("war3map.w3q")),
						gameDataFileSystem.has("war3map.wts") ? new WTSFile(gameDataFileSystem.getResourceAsStream("war3map.wts"))
								: null,
						true);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final DataTable standardMeta = StandardObjectData.getStandardUpgradeMeta();
		final DataTable standardUpgradeEffectMeta = StandardObjectData.getStandardUpgradeEffectMeta();
		final UnitEditorPanel unitEditorPanel = new UnitEditorPanel(
				new MutableObjectData(WorldEditorDataType.UPGRADES, StandardObjectData.getStandardUpgrades(),
						standardMeta, unitDataChangeset),
				standardMeta, new UpgradesFieldBuilder(standardUpgradeEffectMeta), new UpgradeTabTreeBrowserBuilder(),
				WorldEditorDataType.UPGRADES, new EditorTabCustomToolbarButtonData("WESTRING_MENU_OE_UPGR_NEW",
				"ToolBarIcon_OE_NewUpgr", "WESTRING_MENU_OE_UPGR_COPY", "WESTRING_MENU_OE_UPGR_PASTE"),
                () -> {
                    // TODO Auto-generated method stub

                });
		return unitEditorPanel;
	}

	public void saveSpecificTabData() {
		// jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jFileChooser.resetChoosableFileFilters();
		jFileChooser.setAcceptAllFileFilterUsed(false);
		jFileChooser.setDialogTitle("Export Data from this Tab");
		final int selectedIndex = tabbedPane.getSelectedIndex();
		final UnitEditorPanel unitEditorPanel = editors.get(selectedIndex);
		JOptionPane.showMessageDialog(ObjectEditorPanel.this,
				"OK, friend, we are going to export " + unitEditorPanel.getUnitData().getWorldEditorDataType());
		jFileChooser.addChoosableFileFilter(
				new FileNameExtensionFilter(getFileTypeName(unitEditorPanel.getUnitData().getWorldEditorDataType()),
						unitEditorPanel.getUnitData().getWorldEditorDataType().getExtension()));
		final String extension = unitEditorPanel.getUnitData().getWorldEditorDataType().getExtension();
		if (jFileChooser.showSaveDialog(ObjectEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			final File selectedFile = jFileChooser.getSelectedFile();
			if (selectedFile != null) {
				String path = selectedFile.getPath();
				if (!path.toLowerCase().endsWith("." + extension)) {
					path += "." + extension;
				}
				final File w3uFile = new File(path);
				if (w3uFile.exists()) {
					final int result = JOptionPane.showConfirmDialog(ObjectEditorPanel.this,
							w3uFile.getName() + " already exists. Ok to overwrite?", "Warning",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (result != JOptionPane.OK_OPTION) {
						return;
					}
				}
				try {
					try (BlizzardDataOutputStream outputStream = new BlizzardDataOutputStream(w3uFile)) {
						unitEditorPanel.getUnitData().getEditorData().save(outputStream, false);
					}
				} catch (final FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public String getFileTypeName(final WorldEditorDataType dataType) {
		switch (dataType) {
			case ABILITIES:
				return WEString.getString("WESTRING_FILETYPE_ABILITYDATA");
			case BUFFS_EFFECTS:
				return WEString.getString("WESTRING_FILETYPE_BUFFDATA");
			case DESTRUCTIBLES:
				return WEString.getString("WESTRING_FILETYPE_DESTRUCTABLEDATA");
			case DOODADS:
				return WEString.getString("WESTRING_FILETYPE_DOODADDATA");
			case ITEM:
				return WEString.getString("WESTRING_FILETYPE_ITEMDATA");
			case UNITS:
				return WEString.getString("WESTRING_FILETYPE_UNITDATA");
			case UPGRADES:
				return WEString.getString("WESTRING_FILETYPE_UPGRADEDATA");
			default:
				return WEString.getString("WESTRING_UNKNOWN");
		}
	}

	public void openSpecificTabData() {
		// jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jFileChooser.resetChoosableFileFilters();
		jFileChooser.setAcceptAllFileFilterUsed(false);
		jFileChooser.setDialogTitle("Import Data to this Tab");
		final int selectedIndex = tabbedPane.getSelectedIndex();
		final UnitEditorPanel unitEditorPanel = editors.get(selectedIndex);
		JOptionPane.showMessageDialog(ObjectEditorPanel.this,
				"OK, friend, we are going to import " + unitEditorPanel.getUnitData().getWorldEditorDataType()
						+ ". This will replace all settings, like WE.");
		jFileChooser.addChoosableFileFilter(
				new FileNameExtensionFilter(getFileTypeName(unitEditorPanel.getUnitData().getWorldEditorDataType()),
						unitEditorPanel.getUnitData().getWorldEditorDataType().getExtension()));
		final String extension = unitEditorPanel.getUnitData().getWorldEditorDataType().getExtension();
		if (jFileChooser.showOpenDialog(ObjectEditorPanel.this) == JFileChooser.APPROVE_OPTION) {
			final File selectedFile = jFileChooser.getSelectedFile();
			if (selectedFile != null) {
				final String path = selectedFile.getPath();
				final File w3uFile = new File(path);
				if (!w3uFile.exists()) {
					JOptionPane.showMessageDialog(ObjectEditorPanel.this, "Error. Chosen file did not exist. Retry?");
					return;
				}
				try {
					try (BlizzardDataInputStream inputStream = new BlizzardDataInputStream(
							new FileInputStream(w3uFile))) {
						unitEditorPanel.getUnitData().dropCachesHack();
						unitEditorPanel.getUnitData().getEditorData().getCustom().clear();
						unitEditorPanel.getUnitData().getEditorData().getOriginal().clear();
						unitEditorPanel.getUnitData().getEditorData().load(inputStream, null, false);
						unitEditorPanel.reloadAllDataVerySlowly();
					}
				} catch (final FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private final class NewCustomUnitDialogRunner implements Runnable {
		private final MutableObjectData unitData;

		private NewCustomUnitDialogRunner(final MutableObjectData unitData) {
			this.unitData = unitData;
		}

		@Override
		public void run() {
			final JLabel nameLabel = new JLabel(WEString.getString("WESTRING_UE_FIELDNAME") + ":");
			final JTextField nameField = new JTextField(30);
			nameField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			nameField.setPreferredSize(new Dimension(200, 18));
			nameField.setMaximumSize(new Dimension(200, 1000));
			final JLabel baseUnitLabel = new JLabel(WEString.getString("WESTRING_UE_BASEUNIT").replace("&", "") + ":");
			final UnitOptionPanel unitOptionPanel = new UnitOptionPanel(StandardObjectData.getStandardUnits(),
					StandardObjectData.getStandardAbilities(), 2, true, true);
			unitOptionPanel.setPreferredSize(new Dimension(416, 400));
			unitOptionPanel.setSize(new Dimension(416, 400));
			unitOptionPanel.doLayout();
			unitOptionPanel.relayout();
			final JPanel popupPanel = new JPanel();
			final GroupLayout groupLayout = new GroupLayout(popupPanel);
			popupPanel.setLayout(groupLayout);
			groupLayout.setHorizontalGroup(groupLayout
					.createParallelGroup().addGroup(groupLayout.createSequentialGroup().addComponent(nameLabel)
							.addGap(4).addComponent(nameField))
					.addComponent(baseUnitLabel).addComponent(unitOptionPanel));
			groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup().addComponent(nameLabel).addComponent(nameField))
					.addComponent(baseUnitLabel).addComponent(unitOptionPanel));

			final int response = JOptionPane.showConfirmDialog(ObjectEditorPanel.this, popupPanel,
					WEString.getString("WESTRING_UE_CREATECUSTOMUNIT"), JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if (response == JOptionPane.OK_OPTION) {
				final GameObject selection = unitOptionPanel.getSelection();
				final War3ID sourceId = War3ID.fromString(selection.getId());
				final MutableGameObject newObject = unitData.createNew(
						unitData.getNextDefaultEditorId(War3ID.fromString(sourceId.charAt(0) + "000")), sourceId);
				newObject.setField(UNIT_NAME, 0, nameField.getText());
			}
		}
	}
}
