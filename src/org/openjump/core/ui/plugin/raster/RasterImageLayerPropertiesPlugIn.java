package org.openjump.core.ui.plugin.raster;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import javax.media.jai.PlanarImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.TiffTags.TiffReadingException;
import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;
import org.openjump.core.ui.swing.DetachableInternalFrame;
import org.openjump.core.ui.util.ProjUtils;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * @author Giuseppe Aruta (giuseppe_aruta[AT]yahoo.it)
 * @version 0.1 - 2013_05_27 (Giuseppe Aruta) Simple plugin that allows to view
 *          some properties of Sextante Raster Layer: name, file location,
 *          raster dimension (in cell), raster extension, X cell size, numbers
 *          of bands, min-max-mean of 1st band value (if the raster is monoband)
 * @version 0.2 - 2015_01_02 (Giuseppe Aruta) Advanced plugin. Displays File,
 *          Raster and cells data properties of Sextante Raster Layer and allows
 *          to save those information as TXT file
 * @version 0.3 - 2015_01_31. (Giuseppe Aruta) Used HTML instead of TXT frame.
 *          Info can be saved as HTML file
 * @version 0.4 - 2015_03_27. (Giuseppe Aruta) Added Raster Layer statistics tab
 *          with several info about cell values
 * @version 0.5 - 2016_03_23 (Giuseppe Aruta) Enhenced version. It uses basic
 *          GvSIG table format for data display. Added Transparency panel.
 *          Remove statistic panel as already implemented as in the Raster
 *          Statistics plugin. Added a minimal raster projection display:
 *          currently it only displays simple project definition (not EPSG).
 */

public class RasterImageLayerPropertiesPlugIn extends AbstractPlugIn {

	// Components
	private JSlider transparencySlider = new JSlider();
	@SuppressWarnings("rawtypes")
	protected Dictionary sliderLabelDictionary = new Hashtable();
	private JPanel transparencySliderPanel = new JPanel(new GridBagLayout());
	private String layer_name;
	private String file_path;
	private String raster_bands;
	private String raster_datatype;
	private String raster_colordepth;
	private String raster_dpi;
	private int extent_columns;
	private int extent_rows;
	private String extent_cellnumber;
	private String extent_area;
	private String extent_width;
	private String extent_height;
	private String extent_cellSizeX;
	private String extent_cellSizeY;
	private String extent_minX;
	private String extent_maxX;
	private String extent_minY;
	private String extent_maxY;
	private long file_size;
	private String file_sizeMB;
	private String file_type;
	private String raster_nodata;
	private String proj_file_path;
	private String proj_coordinate;

	private final static String PLUGIN_NAME = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn");
	private final static String LAYER_PROPERTIES = I18N
			.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Layer-Properties");
	private static final String INFO = I18N
			.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Info");
	private final static String PROPORTIONAL_TRANSPARENCY_ADJUSTER = I18N
			.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Proportional-Transparency-Adjustment");
	private final static String TRANSPARENCY = I18N
			.get("ui.renderer.style.ColorThemingPanel.transparency");
	private final static String FILE = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.namefile");
	private final static String FILE_NAME = I18N
			.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
	private final static String FILE_TYPE = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.type");
	private final static String FILE_DIMENSION = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension");
	private final static String RASTER = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.raster");
	private final static String RASTER_BANDS = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.bands_number");
	private final static String RASTER_DPI = "DPI";
	private final static String RASTER_DATATYPE = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.datatype");
	private final static String RASTER_COLORDEPTH = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.colordepth");
	private final static String RASTER_NODATA = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cell.nodata");
	private final static String EXTENT = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.extent");
	private final static String EXTENT_XMIN = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.xmin");
	private final static String EXTENT_YMIN = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.ymin");
	private final static String EXTENT_XMAX = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.xmax");
	private final static String EXTENT_YMAX = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.ymax");
	private final static String EXTENT_SIZE = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_raster");
	private final static String EXTENT_CELL_SIZE = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.dimension_cell");
	private final static String EXTENT_CELL_NUM = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.cellnum");
	private final static String EXTENT_AREA = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.area");
	private final static String BAND = I18N
			.get("org.openjump.core.ui.plugin.raster.CreatePolygonGridFromSelectedImageLayerPlugIn.band");
	private static final String LAYER = I18N
			.get("com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn.name");
	private static final String NAME = I18N
			.get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Name");
	private static final String SEXTANTE = I18N
			.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.Sextante-Raster-Image");
	private static final String NOT_SAVED = I18N
			.get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved");
	private static final String CANCEL = I18N.get("ui.OKCancelPanel.cancel");
	private static final String OK = I18N.get("ui.OKCancelPanel.ok");
	private static final String COORDINATE_SYSTEM = I18N
			.get("datasource.FileDataSourceQueryChooser.coordinate-system-of-file");
	private static final String R_MAX = I18N
			.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum");
	private static final String R_MIN = I18N
			.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum");
	private static final String R_MEAN = I18N
			.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.mean");
	private static final String R_STD = I18N
			.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev");
	private static final String GEO_METADATA = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.geographic_metadata");
	private static final String PROJECTION = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.projection");

	private Envelope extent;

	public static MultiEnableCheck createEnableCheck(
			final WorkbenchContext workbenchContext) {
		EnableCheckFactory checkFactory = new EnableCheckFactory(
				workbenchContext);
		MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
		multiEnableCheck.add(checkFactory
				.createExactlyNLayerablesMustBeSelectedCheck(1,
						RasterImageLayer.class));
		return multiEnableCheck;
	}

	public Icon getIcon() {
		return IconLoader.icon("information_16x16.png");
	}

	public String getName() {
		return PLUGIN_NAME;
	}

	private final String bgColor0 = "\"#FEEDD6\""; // light salmon
	private final String bgColor1 = "\"#EAEAEA\""; // light grey
	private final String bgColor3 = "\"#FBFFE1\""; // light yellow
	private final String bgColor4 = "\"#CCCCCC\""; //

	public String title(String textA, String textB) {
		String cabecera = "  <tr valign=\"top\">"
				+ "     <td width=\"400\" height=\"18\" "
				+ "align=\"center\"><font face=\"Arial\" size=\"4\" align=\"right\"><b>"
				+ textA + "</b></font></td>"
				+ "     <td width=\"1586\" height=\"18\" "
				+ "align=\"left\"><font face=\"Arial\" size=\"4\"><b>" + textB
				+ "</b></font></td>" + "  </tr>";
		return cabecera;
	}

	public String header(String textA, String textB) {
		String cabecera = "  <tr valign=\"top\">"
				+ "     <td width=\"400\" height=\"18\" bgcolor="
				+ bgColor3
				+ "align=\"center\"><font face=\"Arial\" size=\"3\" align=\"right\"><b>"
				+ textA + "</b></font></td>"
				+ "     <td width=\"1586\" height=\"18\" bgcolor=" + bgColor3
				+ "align=\"center\"><font face=\"Arial\" size=\"3\"><b>"
				+ textB + "</b></font></td>" + "  </tr>";
		return cabecera;
	}

	public String property(String textA, String textB, String color) {

		String propiedad = "  <tr valign=\"top\">"
				+ "     <td width=\"400\" height=\"18\" bgcolor="
				+ bgColor4
				+ "align=\"right\"><font face=\"Arial\" size=\"3\" align=\"right\">"
				+ textA + "</font></td>"
				+ "     <td width=\"1586\" height=\"18\" bgcolor=" + color
				+ "align=\"left\"><font face=\"Arial\" size=\"3\">" + textB
				+ "</font></td>" + "  </tr>";
		return propiedad;
	}

	public String infoText(RasterImageLayer rLayer)
			throws NoninvertibleTransformException, TiffReadingException,
			Exception {
		String infotext = null;
		setInfo(rLayer);
		setInfoProjection(rLayer);
		int numBands = rLayer.getNumBands();
		String info = "";
		info = info + title(INFO, "");
		info = info + header("", LAYER);
		info = info + property(NAME, layer_name, bgColor0);
		info += header("", FILE);
		if (rLayer.getImageFileName().contains(
				System.getProperty("java.io.tmpdir"))) {
			info += property(FILE_NAME, NOT_SAVED, bgColor1);
		} else {
			info += property(FILE_TYPE, file_type, bgColor0);
			info += property(FILE_NAME, file_path, bgColor1);
			info += property(FILE_DIMENSION, file_sizeMB + " (" + file_size
					+ " bytes)", bgColor0);
			info = info + header("", COORDINATE_SYSTEM);
			info = info + property(PROJECTION, proj_coordinate, bgColor1);
			info = info + property(FILE_NAME, proj_file_path, bgColor0);
		}
		info += header("", EXTENT);
		info += property(EXTENT_XMIN, extent_minX, bgColor0);
		info += property(EXTENT_XMAX, extent_maxX, bgColor1);
		info += property(EXTENT_YMIN, extent_minY, bgColor0);
		info += property(EXTENT_YMAX, extent_maxY, bgColor1);
		info += property(EXTENT_SIZE, extent_width + " x " + extent_height
				+ " (" + EXTENT_AREA + ": " + extent_area + ")", bgColor0);
		info += property(EXTENT_CELL_SIZE, extent_cellSizeX + ", "
				+ extent_cellSizeY, bgColor1);
		info += property(EXTENT_CELL_NUM, extent_cellnumber, bgColor0);
		info += header("", RASTER);
		info += property(RASTER_DPI, raster_dpi, bgColor0);
		info += property(RASTER_DATATYPE, raster_datatype, bgColor1);
		info += property(RASTER_COLORDEPTH, raster_colordepth, bgColor0);
		info += property(RASTER_BANDS, raster_bands, bgColor1);
		if (rLayer.getNumBands() == 1) {
			info += property(RASTER_NODATA, raster_nodata, bgColor0);
		}
		for (int b = 0; b < numBands; b++) {
			int numerobanda = b + 1;
			info += header("", BAND + " - " + numerobanda);
			info += property(R_MIN,
					String.valueOf(rLayer.getMetadata().getStats().getMin(b)),
					bgColor0);
			info += property(R_MAX,
					String.valueOf(rLayer.getMetadata().getStats().getMax(b)),
					bgColor1);
			info += property(R_MEAN,
					String.valueOf(rLayer.getMetadata().getStats().getMean(b)),
					bgColor0);
			info += property(R_STD, String.valueOf(rLayer.getMetadata()
					.getStats().getStdDev(b)), bgColor1);
		}
		String table = "<table border='0.1'>";
		String table2 = "</table>";
		infotext = "<html>" + table + info + table2 + "</html>";
		return infotext;

	}

	@SuppressWarnings("unchecked")
	private JPanel Transparency(final RasterImageLayer layer) {
		transparencySliderPanel.setBorder(BorderFactory
				.createTitledBorder(PROPORTIONAL_TRANSPARENCY_ADJUSTER));
		Box box = new Box(1);
		for (int i = 0; i <= 100; i += 25) {
			sliderLabelDictionary.put(new Integer(i), new JLabel(i + "%"));
		}
		transparencySlider.setMinimumSize(new Dimension(200, 20));
		transparencySlider.setPreferredSize(new Dimension(460, 50));
		transparencySlider.setPaintLabels(true);
		transparencySlider.setPaintTicks(true);
		transparencySlider.setLabelTable(sliderLabelDictionary);
		transparencySlider.setMajorTickSpacing(10);
		transparencySlider.setMinorTickSpacing(5);
		transparencySlider.setMaximum(100);
		transparencySlider.setMinimum(0);
		transparencySlider.setValue((int) (layer.getTransparencyLevel() * 100));
		box.add(transparencySlider);
		transparencySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int newTransparencyValue = transparencySlider.getValue();
					layer.setTransparencyLevelInPercent(newTransparencyValue);
					layer.fireAppearanceChanged();
				}
			}
		});
		transparencySliderPanel.add(box);
		return transparencySliderPanel;
	}

	public boolean execute(final PlugInContext context) throws Exception {
		final RasterImageLayer rLayer = (RasterImageLayer) LayerTools
				.getSelectedLayerable(context, RasterImageLayer.class);
		final double transparency = rLayer.getTransparencyLevel();
		final DetachableInternalFrame frame = new DetachableInternalFrame(
				LAYER_PROPERTIES + ": " + rLayer.getName());
		frame.setIconifiable(true);
		frame.setFrameIcon(IconLoader.icon("information_16x16.png"));
		HTMLPanel infoHTML = new HTMLPanel();
		infoHTML.getRecordPanel().removeAll();
		infoHTML.createNewDocument();
		infoHTML.append(infoText(rLayer));
		JTabbedPane tabbedPane = new JTabbedPane();
		Border mainComponentBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JPanel panelTransparency = new JPanel();
		FormUtils.addRowInGBL(panelTransparency, 1, 0, Transparency(rLayer));
		tabbedPane.setBorder(mainComponentBorder);
		tabbedPane.addTab(INFO, getIcon(), infoHTML, "");
		tabbedPane.addTab(TRANSPARENCY, null, panelTransparency, "");
		// OK- Cancel panel
		final JButton okButton = new JButton(OK) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(100, 25);
			}
		};
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				return;
			}
		});
		final JButton cancelButton = new JButton(CANCEL) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(100, 25);
			}
		};
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rLayer.setTransparencyLevel(transparency);
				transparencySlider.setValue((int) (transparency) * 100);
				frame.dispose();
				return;
			}
		});
		JPanel okCancelPane = new JPanel();
		okCancelPane.add(okButton);
		okCancelPane.add(cancelButton);
		frame.add(tabbedPane, BorderLayout.CENTER);
		frame.add(okCancelPane, BorderLayout.SOUTH);
		frame.setClosable(true);
		frame.setResizable(true);
		frame.setMaximizable(true);
		frame.setSize(550, 400);
		frame.setVisible(true);
		frame.setIcon(true);
		context.getWorkbenchFrame().addInternalFrame(frame, true, true);
		return true;
	}

	/*
	 * Gets all data info
	 */
	private void setInfo(RasterImageLayer rLayer) throws IOException {
		Locale locale = new Locale("en", "UK");
		String pattern = "###.####";
		DecimalFormat df = (DecimalFormat) NumberFormat
				.getNumberInstance(locale);
		df.applyPattern(pattern);
		layer_name = rLayer.getName();// Get name of layer
		file_path = rLayer.getImageFileName();// get file path
		String fileSourcePath = rLayer.getImageFileName();
		String extension = FileUtil.getExtension(fileSourcePath);
		if ((extension.equals("tif") || extension.equals("tiff")
				|| extension.equals("TIF") || extension.equals("TIFF"))
				&& ProjUtils.isGeoTIFF(fileSourcePath)) {
			file_type = "GeoTIFF" + " - " + SEXTANTE;// Get GeoTIF
														// description
		} else {
			file_type = fileExtension(rLayer) + " - " + SEXTANTE;// Get file
																	// description
		}
		file_size = getFileSizeBytes(rLayer); // Get file size in byte
		file_sizeMB = getFileSizeMegaBytes(file_size); // Get file size in Mega
														// Bytes
		raster_bands = df.format(rLayer.getNumBands());// Get raster number of
														// bands
		raster_datatype = getDataType(rLayer);// Get raster data type
		raster_colordepth = getColorDepth(rLayer);// Get raster color depth
		raster_dpi = getDPI(rLayer);// Get raster DPI
		extent = rLayer.getWholeImageEnvelope(); // Get Envelope
		extent_cellSizeX = df.format(cellSizeX(rLayer));// Get X Cell size
		extent_cellSizeY = df.format(cellSizeY(rLayer));// Get Y Cell size
		extent_columns = getNumColumns(rLayer); // Get Number of columns
		extent_rows = getNumRows(rLayer); // Get Number of rows
		extent_cellnumber = String.valueOf(extent_columns * extent_rows); // Get
																			// cell
																			// number
		extent_area = df.format(rLayer.getWholeImageEnvelope().getArea()); // Get
																			// Area
		extent_width = df.format(rLayer.getWholeImageEnvelope().getWidth()); // Get
																				// Width
		extent_height = df.format(rLayer.getWholeImageEnvelope().getHeight()); // Get
																				// Height
		extent_minX = df.format(extent.getMinX());
		extent_maxX = df.format(extent.getMaxX());
		extent_minY = df.format(extent.getMinY());
		extent_maxY = df.format(extent.getMaxY());
		raster_nodata = String.valueOf(rLayer.getNoDataValue());
		int numBands = rLayer.getNumBands();
		raster_bands = String.valueOf(numBands);
		for (int b = 0; b < numBands; b++) {
			int numerobanda = b + 1;
			String.valueOf(numerobanda);
			String.valueOf(rLayer.getMetadata().getStats().getMax(b));
			String.valueOf(rLayer.getMetadata().getStats().getMin(b));
			String.valueOf(rLayer.getMetadata().getStats().getMean(b));
			String.valueOf(rLayer.getMetadata().getStats().getStdDev(b));
		}
	}

	/*
	 * Get Projection of selected raster. First it checks if selected raster is
	 * a GeoTIF and scan tiff tags for projection. If selected file is not a
	 * GeoTIF, it checks if <Filename>.AUX.XML exists and scans inside it. As
	 * last choice it scans into <filename>.PRJ file
	 */
	private void setInfoProjection(RasterImageLayer layer) throws IOException {
		String fileSourcePath = layer.getImageFileName();
		String extension = FileUtil.getExtension(fileSourcePath);
		if ((extension.equals("tif") || extension.equals("tiff")
				|| extension.equals("TIF") || extension.equals("TIFF"))) {
			if (ProjUtils.isGeoTIFF(fileSourcePath)) {
				proj_file_path = GEO_METADATA;
				proj_coordinate = ProjUtils
						.getGeoTiffProjection(fileSourcePath);
			} else {
				proj_file_path = ProjUtils
						.getFileProjectionPath(fileSourcePath);
				proj_coordinate = ProjUtils.getFileProjection(fileSourcePath);
			}
		} else if ((extension.equals("asc") || extension.equals("ASC")
				|| extension.equals("FLT") || extension.equals("flt"))) {
			proj_file_path = ProjUtils.getFileProjectionPath(fileSourcePath);
			proj_coordinate = ProjUtils.getFileProjection(fileSourcePath);
		} else {
			proj_file_path = ProjUtils.getFileProjectionPath(fileSourcePath);
			proj_coordinate = ProjUtils.getFileProjection(fileSourcePath);
		}
	}

	// //////////////////////////////////////////

	private int datatype;
	private String type;

	private static final String[] Q = new String[] { "", "KB", "MB", "GB",
			"TB", "PB", "EB" };

	/*
	 * Converts bytes to multiple (Kilo, Mega, Giga-bytes according to the
	 * dimension of the file
	 */
	public String getFileSizeMegaBytes(long bytes) {
		for (int i = 6; i > 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.1f %s", bytes / step, Q[i]);
		}
		return Long.toString(bytes);
	}

	/*
	 * Gets dimension of file in bytes
	 */
	public long getFileSizeBytes(RasterImageLayer layer) {
		File rfile = new File(layer.getImageFileName());
		return rfile.length();
	}

	/*
	 * Return the extension of the file as String
	 */
	public String fileExtension(RasterImageLayer layer) {
		File f = new File(layer.getImageFileName());
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toUpperCase();
		}
		return ext;
	}

	/*
	 * Gets the number of bands
	 */
	public String numBands(RasterImageLayer layer) {
		int bands = layer.getNumBands();
		return String.valueOf(bands);
	}

	/*
	 * Gets data type
	 */
	public String getDataType(RasterImageLayer rLayer) throws IOException {
		Raster r = rLayer.getRasterData(null);
		SampleModel sm = r.getSampleModel();
		datatype = sm.getDataType();
		switch (datatype) {
		case DataBuffer.TYPE_BYTE: {
			type = "byte";
			break;
		}
		case DataBuffer.TYPE_SHORT: {
			type = "short";
			break;
		}
		case DataBuffer.TYPE_USHORT: {
			type = "ushort";
			break;
		}
		case DataBuffer.TYPE_INT: {
			type = "int";
			break;
		}
		case DataBuffer.TYPE_FLOAT: {
			type = "float";
			break;
		}
		case DataBuffer.TYPE_DOUBLE: {
			type = "double";
			break;
		}
		case DataBuffer.TYPE_UNDEFINED: {
			type = "undefined";
			break;
		}
		}
		return type;
	}

	/*
	 * Gets color depth
	 */
	public String getColorDepth(RasterImageLayer layer) throws IOException {
		Raster r = null;
		r = layer.getRasterData(null);
		SampleModel sm = r.getSampleModel();
		ColorModel cm = PlanarImage.createColorModel(sm);
		int colordepth = cm.getNumComponents();
		String color = String.valueOf(colordepth) + " bpp";
		return color;
	}

	/*
	 * Gets Dots per Inch (DPI)
	 */
	public String getDPI(RasterImageLayer layer) throws IOException {
		Raster r = null;
		r = layer.getRasterData(null);
		SampleModel sm = r.getSampleModel();
		ColorModel cm = PlanarImage.createColorModel(sm);
		String color = String.valueOf(cm.getPixelSize());
		return color;
	}

	/*
	 * Gets cell size
	 */
	public double cellSizeX(RasterImageLayer layer) throws IOException {
		Raster m_Raster = layer.getRasterData(null);
		Envelope env = layer.getWholeImageEnvelope();
		double cellSize = env.getWidth() / (double) m_Raster.getWidth();
		return cellSize;
	}

	public double cellSizeY(RasterImageLayer layer) throws IOException {
		Raster m_Raster = layer.getRasterData(null);
		Envelope env = layer.getWholeImageEnvelope();
		double cellSize = env.getHeight() / (double) m_Raster.getHeight();
		return cellSize;

	}

	/*
	 * Gets number of columns
	 */
	public int getNumColumns(RasterImageLayer layer) throws IOException {
		Raster m_Raster = layer.getRasterData(null);
		int x = m_Raster.getWidth();
		return x;
	}

	/*
	 * Gets number of rows
	 */
	public int getNumRows(RasterImageLayer layer) throws IOException {
		Raster m_Raster = layer.getRasterData(null);
		int x = m_Raster.getHeight();
		return x;

	}

	/*
	 * Counts the number of no data cells. This code is deactivated
	 */
	public int getNodataCellsNumber(RasterImageLayer rLayer) throws IOException {
		OpenJUMPSextanteRasterLayer rstLayer = new OpenJUMPSextanteRasterLayer();
		rstLayer.create(rLayer);
		int counter = 0;
		GridWrapperNotInterpolated gwrapper = new GridWrapperNotInterpolated(
				rstLayer, rstLayer.getLayerGridExtent());
		int nx = rstLayer.getLayerGridExtent().getNX();
		int ny = rstLayer.getLayerGridExtent().getNY();
		for (int y = 0; y < ny; y++) {
			for (int x = 0; x < nx; x++) {
				double value = gwrapper.getCellValueAsDouble(x, y, 0);
				if (value == rstLayer.getNoDataValue())
					counter++;
			}
		}
		return counter;
	}

	/*
	 * Counts the number of valid cells. This code is deactivated
	 */
	public int getValidCellsNumber(RasterImageLayer rLayer) throws IOException {
		int number = getNumColumns(rLayer) * getNumRows(rLayer)
				- getNodataCellsNumber(rLayer);
		return number;
	}

}
