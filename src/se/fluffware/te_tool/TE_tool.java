package se.fluffware.te_tool;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.fife.ui.rtextarea.*;

import se.fluffware.grayhill.te.project.BinaryProject;
import se.fluffware.grayhill.te.project.Image;
import se.fluffware.grayhill.te.project.PackageFile;
import se.fluffware.grayhill.te.project.Project;
import se.fluffware.grayhill.te.project.Resources;
import se.fluffware.grayhill.te.project.Ring;
import se.fluffware.grayhill.te.project.Screen;
import se.fluffware.grayhill.te.project.Text;
import se.fluffware.grayhill.te.project.Widget;
import se.fluffware.grayhill.te.project.XMLProject;

import org.fife.ui.rsyntaxtextarea.*;

public abstract class TE_tool {

	
	static public class XMLEditor extends JFrame {
		Preferences prefs = Preferences.userRoot();
		File opened_file = null;
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		RSyntaxTextArea textArea;

		class FileHandler extends TransferHandler
		{

			TransferHandler chained;
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public FileHandler(TransferHandler h)
			{
				chained = h;
			}
			public boolean canImport(JComponent comp,
	                DataFlavor[] transferFlavors)
			{
				for (DataFlavor df : transferFlavors) {
					if (df.isFlavorJavaFileListType()) {
						return true;
					}
				}
				return chained.canImport(comp, transferFlavors);
			}
			
			public boolean canImport(TransferHandler.TransferSupport support) {

				for (DataFlavor df : support.getDataFlavors()) {
					if (df.isFlavorJavaFileListType()) {
						return true;
					}
				}
				return chained.canImport(support);
			}

			public boolean importData(TransferHandler.TransferSupport support) {
				for (DataFlavor df : support.getDataFlavors()) {
					if (df.isFlavorJavaFileListType()) {
						Transferable t = support.getTransferable();
						try {
							@SuppressWarnings("unchecked")
							List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
							if (files.size() >= 1) {
								loadFile(files.get(0));
								System.err.println(files.get(0));
							}

						} catch (UnsupportedFlavorException e) {
							return false;
						} catch (IOException e) {
							return false;
						}
						return false;
					}
				}
				return chained.importData(support);
			}

			public void exportAsDrag(JComponent comp, InputEvent e, int action) {
				chained.exportAsDrag(comp, e, action);
			}

			public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
				chained.exportToClipboard(comp, clip, action);
			}
			
			public java.awt.Image getDragImage()
			{
				return chained.getDragImage();
			}
			
			public Point getDragImageOffset()
			{
				return chained.getDragImageOffset();
			}
			
			public int getSourceActions(JComponent c)
			{
				return chained.getSourceActions(c);
			}
			public Icon getVisualRepresentation(Transferable t)
			{
				return chained.getVisualRepresentation(t);
			}
			
			public void setDragImage(java.awt.Image img)
			{
				chained.setDragImage(img);
			}
			
			public void setDragImageOffset(Point p)
			{
				chained.setDragImageOffset(p);
			}
		}
		
		void loadFile(File file)
		{
			try {
				FileInputStream in = new FileInputStream(file);
				InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
				textArea.read(reader, file);
				reader.close();
				opened_file = file;
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						documentChanged = false;
						
						setTitle();
					}
				});
				
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(XMLEditor.this, file.getPath() + " does not exist");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(XMLEditor.this, "Failed to read " + file.getPath());
			}
		}
		
		void saveFile(File file)
		{
			try {
				FileOutputStream out = new FileOutputStream(file);
				OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));
				textArea.write(writer);
				opened_file = file;
				prefs.put(UserPrefs.CurrentProjDir, file.toString());
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						documentChanged = false;
						
						setTitle();
					}
				});
				
			} catch (IOException e) {
				JOptionPane.showMessageDialog(XMLEditor.this, "Failed to write " + file.getPath()+": "+e.getMessage());
			}
		}
		
		class OpenAction extends AbstractAction {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public OpenAction() {
				putValue(NAME, "Open");
				putValue(ACCELERATOR_KEY,
						KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
				putValue(MNEMONIC_KEY, KeyEvent.VK_O);
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Project definitions", "XML");
				chooser.setFileFilter(filter);
				File default_dir = new File(prefs.get(UserPrefs.CurrentProjDir, null));	
				chooser.setCurrentDirectory(default_dir);
				int returnVal = chooser.showOpenDialog(XMLEditor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					loadFile(file);

				}
			}

		}

		class SaveAction extends AbstractAction {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public SaveAction() {
				putValue(NAME, "Save");
				putValue(ACCELERATOR_KEY,
						KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
				putValue(MNEMONIC_KEY, KeyEvent.VK_S);
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Project definitions", "XML");
				chooser.setFileFilter(filter);
				if (opened_file != null) {
					chooser.setSelectedFile(opened_file);
				} else {
					File default_dir = new File(prefs.get(UserPrefs.CurrentProjDir, "."));	
					chooser.setCurrentDirectory(default_dir);
				}
				int returnVal = chooser.showSaveDialog(XMLEditor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					saveFile(file);
				}
			}

		}

		class ExportAction extends AbstractAction {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public ExportAction() {
				putValue(NAME, "Export");
				putValue(ACCELERATOR_KEY,
						KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_DOWN_MASK 
								| java.awt.event.InputEvent.SHIFT_DOWN_MASK));
				putValue(MNEMONIC_KEY, KeyEvent.VK_E);
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					StringReader xml_text = new StringReader(textArea.getText());

					XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(xml_text);

					Project proj = XMLProject.readProject(xml);
					if (opened_file != null) {
						File proj_dir = opened_file.getParentFile();
						File bin_proj = proj_dir.toPath().resolve("package").resolve("updateProject.mdp").toFile();
						try {
							BinaryProject.saveProject(bin_proj, proj);
						} catch (IOException e) {
							JOptionPane.showMessageDialog(XMLEditor.this,
									"Failed to save binary project file "+bin_proj.getName()+": " + e.getMessage());
						}
					}
				} catch (XMLStreamException e) {
					JOptionPane.showMessageDialog(XMLEditor.this, "Failed to read XML document: "+e.getMessage());
				}
				
			}

		}
		
		class ImportAction extends AbstractAction {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public ImportAction() {
				putValue(NAME, "Import");
				putValue(ACCELERATOR_KEY,
						KeyStroke.getKeyStroke(KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK 
								| java.awt.event.InputEvent.SHIFT_DOWN_MASK));
				putValue(MNEMONIC_KEY, KeyEvent.VK_E);
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Import project");
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Binary project or package", "mdp", "zip");
				chooser.setFileFilter(filter);
				File default_dir = new File(prefs.get(UserPrefs.ImportDir, "."));	
				chooser.setCurrentDirectory(default_dir);
				int returnVal = chooser.showOpenDialog(XMLEditor.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					prefs.put(UserPrefs.ImportDir, file.getParent().toString());
					System.err.println("Path: " + file.toPath().toString());
					final File proj_file;
					if (file.toString().endsWith(".zip")) {
						JFileChooser dest_chooser = new JFileChooser();
						dest_chooser.setDialogTitle("Project directory");
						dest_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						File proj_dir = new File(prefs.get(UserPrefs.CurrentProjDir, "."));	
						dest_chooser.setCurrentDirectory(proj_dir);
						int destReturnVal = dest_chooser.showOpenDialog(XMLEditor.this);
						if (destReturnVal != JFileChooser.APPROVE_OPTION) {
							return;
						}
						File dest_dir = dest_chooser.getSelectedFile();
						prefs.put(UserPrefs.CurrentProjDir, dest_dir.toString());
						File package_dir = dest_dir.toPath().resolve("package").toFile();
						if (!package_dir.isDirectory() && !package_dir.mkdir()) {
							JOptionPane.showMessageDialog(XMLEditor.this, "Failed to create package directory");
							return;
						}
						try {
							PackageFile.unpackToDir(file, package_dir);
						} catch (IOException e) {
							JOptionPane.showMessageDialog(XMLEditor.this,
									"Failed to unpack package: " + e.getMessage());
							return;
						}
						file = package_dir.toPath().resolve("updateProject.mdp").toFile();
						if (!file.exists()) {
							JOptionPane.showMessageDialog(XMLEditor.this,
									"File " + file.getName() + " does not exist in package");
							return;
						}
						proj_file = dest_dir.toPath().resolve("proj.xml").toFile();
						
					} else {
						proj_file = null;
					}
					Project proj;
					try {
						proj = BinaryProject.loadProject(file);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(XMLEditor.this, "Failed to read poject file: "+e.getMessage());
						return;
					} catch (BinaryProject.Exception e) {
						JOptionPane.showMessageDialog(XMLEditor.this, "Failed to parse poject file: "+e.getMessage());
						return;
					} 
					
					if (proj_file != null) {
						// Move package images to project root
						HashSet<String> file_names = new HashSet<String>();
						for (Screen s : proj.screens) {
							for (Widget w : s.widgets) {
								if (w instanceof Image) {
									file_names.add(((Image) w).filename);
								} else if (w instanceof Ring) {
									Ring ring = (Ring) w;
									file_names.add(ring.emptyRingImage);
									file_names.add(ring.fullRingImage);
									file_names.add(ring.cursorImage);
								} else if (w instanceof Text) {
									file_names.add(Resources.fontName(((Text) w).fontIndex));
								}
							}
						}
						Path proj_path = proj_file.getParentFile().toPath();
						Path image_path = proj_path.resolve("package").resolve("updateImages");
						for (String s : file_names) {
							File from = image_path.resolve(s).toFile();
							File to = proj_path.resolve(s).toFile();
							if (!from.exists()) {
								JOptionPane.showMessageDialog(XMLEditor.this, "Couldn't find project file " + s);
							} else {
								System.err.println(s);
								if (!from.renameTo(to)) {
									JOptionPane.showMessageDialog(XMLEditor.this,
											"Couldn't move " + from.toString() + " to " + toString());
									return;
								}
							}
						}
					}
					StringWriter xml_text = new StringWriter();
					try {
						XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(xml_text);
					
						XMLProject.writeProject(xml, proj);
						
						textArea.setText(xml_text.toString());
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								documentChanged = false;
								opened_file = proj_file;
								if (proj_file != null) {
									saveFile(proj_file);
								}
								setTitle();
							}
						});
					} catch (XMLStreamException e) {
						JOptionPane.showMessageDialog(XMLEditor.this, "Failed to generate XML file: "+e.getMessage());
						return;
					}
				}
				
			}

		}
		public void setTitle()
		{
			String file_name;
			if (opened_file != null) {
				file_name = opened_file.getName();
			} else {
				file_name = "Untitled";
			}
			String title = "Touch Encoder Tool ["+file_name+(documentChanged ? "*":"")+"]";
			setTitle(title);
			setName(title);
		}

		public boolean documentChanged = false;

		class DocumentChanged implements Runnable {
			public void run() {
				if (!documentChanged) {
					documentChanged = true;
					setTitle();
				}
			}
		}
		public Runnable documentChangedRunnable = new DocumentChanged();
		
		class ChangeListener implements DocumentListener
		{

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				SwingUtilities.invokeLater(documentChangedRunnable);
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				SwingUtilities.invokeLater(documentChangedRunnable);	
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				SwingUtilities.invokeLater(documentChangedRunnable);
			}
			
		}
		
		public XMLEditor() {
			JMenuBar menubar = new JMenuBar();
			JMenu file_menu = new JMenu("Project");
			
			JMenuItem open_item = new JMenuItem(new OpenAction());
			file_menu.add(open_item);
			
			JMenuItem save_item = new JMenuItem(new SaveAction());
			file_menu.add(save_item);
			
			JMenuItem export_item = new JMenuItem(new ExportAction());
			file_menu.add(export_item);
			
			JMenuItem import_item = new JMenuItem(new ImportAction());
			file_menu.add(import_item);
			
			menubar.add(file_menu);
			setJMenuBar(menubar);
			JPanel cp = new JPanel(new BorderLayout());

			textArea = new RSyntaxTextArea(20, 60);
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			textArea.setCodeFoldingEnabled(true);
			textArea.setAutoIndentEnabled(true);
			textArea.setAnimateBracketMatching(true);

			RTextScrollPane sp = new RTextScrollPane(textArea);
			cp.add(sp);

			setContentPane(cp);
			
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			pack();
			setLocationRelativeTo(null);

			TransferHandler h = textArea.getTransferHandler();
			textArea.setTransferHandler(new FileHandler(h));
			documentChanged = false;
			textArea.getDocument().addDocumentListener(new ChangeListener());
			setTitle();
		}
	}

	public static void main(String[] args) {
		System.err.println("Start");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new XMLEditor().setVisible(true);
			}
		});
	}

}
