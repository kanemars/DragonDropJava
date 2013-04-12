package rachelandkane;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.SimpleLayout;

public class DragonDropMain {

//	static String ISRUNNING = "ISRUNNING4";
	static String newline = System.getProperty("line.separator");
	static int newlineLength = newline.length();
	static Display display = new Display();
	static Color yellow = display.getSystemColor(SWT.COLOR_YELLOW);
	final Clipboard cb = new Clipboard(display);
	TextTransfer textTransfer = TextTransfer.getInstance();
	Shell shell = new Shell(display);
	Composite top = new Composite (shell, SWT.FILL);
	static Logger log = Logger.getLogger("DragonDropMain");
	static Appender myAppender = new ConsoleAppender(new SimpleLayout());
	// int[] lastSelectedItems;// = list.getSelectionIndices();
	String lastSelectedString;
	Text searchText = new Text(top, SWT.SEARCH | SWT.BORDER);	
	Button quitButton = new Button (top, SWT.PUSH);
	TabFolder tabFolder = new TabFolder(shell, SWT.FILL);
	java.awt.Image awtImage;
	final PopupMenu popup = new PopupMenu();
	final Frame frame = new Frame("");
	String filePath;

	public DragonDropMain(String filePath, String externalFileEditorExe) throws IOException {
		log.addAppender(myAppender);
		log.debug("Attempting to start Dragon Drop 2");
//		if (isRunning ()) {
//			log.debug("DragonDrop already started!");
//			System.exit(1);
//		}
//		onStart ();
		if (externalFileEditorExe != null)
			Runtime.getRuntime().exec(String.format(externalFileEditorExe + " %s", filePath));
		this.filePath = filePath;
		Image image = new Image(display, getClass().getResourceAsStream("dragondrop.jpg"));
		awtImage = Images.convertToAWT(image.getImageData());

		createTrayIcon(awtImage);

		shell.setLayout(new GridLayout(1, false));
		shell.setText("Dragon Drop 2");
		shell.setImage(image);
		shell.setLocation(0, 0);

		shell.addListener(SWT.Close, new Listener() {

			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				event.doit = false;
				shell.setVisible(false);
			}
		});

		top.setLayout(new GridLayout (2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		searchText.setToolTipText("Start typing in here, and any matching strings will be highlighted immediately");
		searchText.addListener(SWT.KeyUp, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				String lowercase = searchText.getText().toLowerCase();
				log.debug("Entered into searchText:" + lowercase);
				if (lowercase.length() > 1) {
					int selectionCount = markSelections(tabFolder.getItem(tabFolder.getSelectionIndex()),
							searchText.getText());
					if (selectionCount == 0) {
						for (int i = 0; i < tabFolder.getItemCount(); i++) {
							if (i != tabFolder.getSelectionIndex()) {
								selectionCount = markSelections(tabFolder.getItem(i), searchText.getText());
								if (selectionCount > 0) {
									tabFolder.setSelection(i);
									break;
								}
							}
						}
					}
				}
			}
		});

		quitButton.setText("Quit");
		quitButton.setToolTipText("Click this button to fully quit the application, including the system tray");
		quitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				quitApplication ();
			}
		});
		//quitButton.pack();

		// GridData gridData = new GridData();
		// gridData.horizontalAlignment = SWT.FILL;
		// gridData.grabExcessHorizontalSpace = true;
		//top.pack();
		
		FileInputStream fstream = new FileInputStream(filePath);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = br.readLine();
		StyledText list;
		if (strLine.startsWith("#")) {
			list = addTabItem (strLine.substring(1));
		} else {
			list = addTabItem ("Main");
			list.append(strLine + newline);
		}
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("#")) {
				list = addTabItem(strLine.substring(1));
			} else {
				list.append(strLine + newline);
			}
		}

		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	public void showShell() {
		shell.setVisible(true);
	}

	/**
	 * 
	 * @param tabItem
	 * @param selection
	 * @return Number of selections made
	 */
	public static int markSelections(TabItem tabItem, String selection) {
		String lowercase = selection.toLowerCase();
		StyledText styledText = (StyledText) tabItem.getControl();
		int searchStart = 0;
		int position = styledText.getText().toLowerCase().indexOf(lowercase, searchStart);
		List<StyleRange> selections = new ArrayList<StyleRange>();
		while (position != -1) {
			selections.add(new StyleRange(position, lowercase.length(), null, yellow));
			position = styledText.getText().toLowerCase().indexOf(lowercase, position + 1);
		}
		styledText.setStyleRanges(selections.toArray(new StyleRange[0]));
		return selections.size();

	}

	public void createTrayIcon(java.awt.Image image) throws IOException {
		final TrayIcon trayIcon = new TrayIcon(image, "Right click on this tray icon to exit Dragon Drop");
		if (SystemTray.isSupported()) {
			frame.setUndecorated(true);
			SystemTray tray = SystemTray.getSystemTray();

			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Display.getDefault().syncExec(new Runnable() {

						@Override
						public void run() {
							shell.setVisible(true);
							shell.forceActive(); // Put it on the top
						}
					});
				}
			});

			// Create a pop-up menu components
			final PopupMenu popup = createTrayPopupMenu();

			trayIcon.setPopupMenu(popup);
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e))
						frame.add(popup);
					// popup.show(frame, e.getXOnScreen(), e.getYOnScreen());
				}
			});
			try {
				frame.setResizable(false);
				tray.add(trayIcon);
			} catch (AWTException e) {
				log.error("TrayIcon could not be added", e);
			}
		}
	}

	protected PopupMenu createTrayPopupMenu() {
		final PopupMenu popup = new PopupMenu();
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				quitApplication ();
			}
		});
		popup.add(exitItem);
		return popup;
	}

	private StyledText addTabItem(String tabTitle) {
		TabItem tabItem = new TabItem(tabFolder, SWT.BORDER);
		tabItem.setText(tabTitle);

		final StyledText styledText = new StyledText(tabFolder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		styledText.addMouseListener(new MouseListener() {

			int mouseDownStart;
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
				log.info("Mouse down: " + styledText.getCaretOffset());
				mouseDownStart = styledText.getCaretOffset();
//				int lineStart = styledText.getCaretOffset();
//				int lineEnd = styledText.getCaretOffset();
//				while (lineStart > 0 && !styledText.getText(lineStart, lineEnd).startsWith(newline))
//					lineStart--;
//
//				// Adjustments
//				if (styledText.getText(lineStart, lineEnd).startsWith(newline))
//					lineStart += newlineLength;
//
//				// styledText.getText(lineStart, end)
//				lineEnd = styledText.getText().indexOf(newline, lineEnd);
//
//				String lineSelected = styledText.getText(lineStart, lineEnd).trim();
//				;
//				log.info("Line Selected is: " + lineSelected);
//
//				cb.setContents(new Object[] { lineSelected }, new Transfer[] { textTransfer });
//
//				styledText.setSelection(lineStart, lineEnd);
			}

			/**
			 * Select the line if the mouseUp happens in the same place as to when mouseDown happened
			 */
			@Override
			public void mouseUp(MouseEvent arg0) {
				log.info("Mouse up: " + styledText.getCaretOffset());
				boolean mouseMoved = mouseDownStart != styledText.getCaretOffset();
				log.info("Mouse moved: " + mouseMoved);
				if (mouseMoved) {
					cb.setContents(new Object[] {styledText.getSelectionText()}, new Transfer[] { textTransfer });
				} else {					
					// Select the line
					int lineStart = mouseDownStart;
					int lineEnd = mouseDownStart;
					while (lineStart > 0 && !styledText.getText(lineStart, lineEnd).startsWith(newline))
						lineStart--;
	
					// Adjustments
					if (styledText.getText(lineStart, lineEnd).startsWith(newline))
						lineStart += newlineLength;
	
					// styledText.getText(lineStart, end)
					lineEnd = styledText.getText().indexOf(newline, lineEnd);
	
					String lineSelected = styledText.getText(lineStart, lineEnd).trim();
					log.info("Line Selected is: " + lineSelected);
	
					cb.setContents(new Object[] { lineSelected }, new Transfer[] { textTransfer });
	
					styledText.setSelection(lineStart, lineEnd);					
				}
			}
		});

		int operations = DND.DROP_MOVE | DND.DROP_COPY;

		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		DragSource source = new DragSource(styledText, operations);
		source.setTransfer(types);
		source.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent e) {
				// Only start the drag if there is actually text in the
				// label - this text will be what is dropped on the target.
				// if (label.getText().length() == 0) {
				// event.doit = false;
				// }
				log.info("dragStart: ");
				// styledText.setSelection(lastSelectedItems);
			};

			public void dragSetData(DragSourceEvent event) {
				// A drop has been performed, so provide the data of the
				// requested type.
				// (Checking the type of the requested data is only
				// necessary if the drag source supports more than
				// one data type but is shown here as an example).
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					// event.data = getSelectedItems(list);
					event.data = cb.getContents(textTransfer);
				}
			}

			public void dragFinished(DragSourceEvent event) {
			}
		});

		tabItem.setControl(styledText);
		return styledText;

	}

	// http://premierspasenjava.blogspot.co.uk/2012/04/check-if-application-is-already-running.html
//	private final void onStart() {
//		Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
//		prefs.put(ISRUNNING, "true");
//	}
//
	private final void quitApplication() {
//		Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
//		prefs.put(ISRUNNING, "false");
		System.exit(0);
	}
//
//	public boolean isRunning() {
//		Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
//		return prefs.get(ISRUNNING, null) != null ? Boolean.valueOf(prefs.get(ISRUNNING, null)) : false;
//	}

	
	// First argument is text filename
	// Second argument is external file editor that will launch editable version
	// of filename
	public static void main(String[] args) {
		if (Desktop.isDesktopSupported())
			try {
				if (args.length > 0) {
					if (args.length > 1) {
						new DragonDropMain(args[0], args [1]);
					} else {
						new DragonDropMain(args[0], null);
					}
				}
			} catch (IOException e) {
				log.error("Can't launch because: ", e);
			}
	}
}
