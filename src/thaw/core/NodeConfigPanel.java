package thaw.core;

import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * NodeConfigPanel. Creates and manages the panel containing all the things to configure
 *  the settings to access the node.
 */
public class NodeConfigPanel implements Observer {
	private Core core;
	private JPanel nodeConfigPanel = null;


	private final static String[] paramNames = {
		I18n.getMessage("thaw.config.nodeAddress"),
		I18n.getMessage("thaw.config.nodePort"),
		I18n.getMessage("thaw.config.maxSimultaneousDownloads"),
		I18n.getMessage("thaw.config.maxSimultaneousInsertions"),
		I18n.getMessage("thaw.config.maxUploadSpeed"),
		I18n.getMessage("thaw.config.thawId")
	};

	private final static String[] configNames = {
		"nodeAddress",
		"nodePort",
		"maxSimultaneousDownloads",
		"maxSimultaneousInsertions",
		"maxUploadSpeed",
		"thawId"
	};

	private final static String[] currentValues = new String[6];


	private final JLabel[] paramLabels = new JLabel[NodeConfigPanel.paramNames.length];
	private final JTextField[] paramFields = new JTextField[NodeConfigPanel.configNames.length];

	private JCheckBox multipleSockets = null;
	private ConfigWindow configWindow = null;


	public NodeConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;
		this.configWindow = configWindow;

		nodeConfigPanel = new JPanel();
		nodeConfigPanel.setLayout(new GridLayout(15, 1));

		for(int i=0; i < NodeConfigPanel.paramNames.length ; i++) {
			String value;

			if( (value = core.getConfig().getValue(NodeConfigPanel.configNames[i])) == null)
				value = "";

			paramLabels[i] = new JLabel(NodeConfigPanel.paramNames[i]);
			paramFields[i] = new JTextField(value);
			NodeConfigPanel.currentValues[i] = value;

			nodeConfigPanel.add(paramLabels[i]);
			nodeConfigPanel.add(paramFields[i]);
		}

		multipleSockets = new JCheckBox(I18n.getMessage("thaw.config.multipleSockets"),
						Boolean.valueOf(core.getConfig().getValue("multipleSockets")).booleanValue());
		nodeConfigPanel.add(new JLabel(" "));
		nodeConfigPanel.add(multipleSockets);

		setVisibility(Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		configWindow.addObserver(this);
	}

	public JPanel getPanel() {
		return nodeConfigPanel;
	}

	private void setVisibility(final boolean advancedMode) {
		for(int i= 2; i < NodeConfigPanel.paramNames.length;i++) {
			paramLabels[i].setVisible(advancedMode);
			paramFields[i].setVisible(advancedMode);
		}

		multipleSockets.setVisible(advancedMode);
	}


	public boolean hasAValueChanged() {
		for(int i=0; i < NodeConfigPanel.paramNames.length ; i++) {
			if (!paramFields[i].getText().equals(NodeConfigPanel.currentValues[i]))
				return true;
		}

		if ((core.getConfig().getValue("multipleSockets") == null)
		    || !core.getConfig().getValue("multipleSockets").equals(Boolean.toString(multipleSockets.isSelected())))
			return true;

		return false;
	}


	public void update(final Observable o, final Object arg) {
		if(arg == core.getConfigWindow().getOkButton()) {
			if (hasAValueChanged())
				configWindow.willNeedConnectionReset();

			for(int i=0;i < NodeConfigPanel.paramNames.length;i++) {
				core.getConfig().setValue(NodeConfigPanel.configNames[i], paramFields[i].getText());
			}

			core.getConfig().setValue("multipleSockets", Boolean.toString(multipleSockets.isSelected()));

			setVisibility(Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());
		}


		if(arg == core.getConfigWindow().getCancelButton()) {
			for(int i=0;i < NodeConfigPanel.paramNames.length;i++) {
				String value;

				if( (value = core.getConfig().getValue(NodeConfigPanel.configNames[i])) == null)
					value = "";

				paramFields[i].setText(value);
			}

			multipleSockets.setSelected(Boolean.valueOf(core.getConfig().getValue("multipleSockets")).booleanValue());
		}
	}

}
