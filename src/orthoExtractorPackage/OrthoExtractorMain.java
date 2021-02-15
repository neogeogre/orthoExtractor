package orthoExtractorPackage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.coords.UTMCoord;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.layertree.LayerTree;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.util.HotSpotController;

/**
 * This program call the World wind globe AppFrame only to get the Bing map of the area of interest.
 * @author Geoffrey
 */
public class OrthoExtractorMain extends ApplicationTemplate
{
	static String CurrentDirectory;
	static List<Sector> SectorForCrop;
	
    public static void main(String[] args)
    {
//    	@SuppressWarnings("resource")
//		Scanner keyboard = new Scanner(System.in);
//		
//		System.out.println("Now enter upper left longitude WGS84 :");
//		String upperLeftLongitudeS = keyboard.nextLine();
    	
    	double upperLeftLongitude = Double.parseDouble(args[0]);
		System.out.println("You entered : " + upperLeftLongitude);
		System.out.println(" ");
		
		double upperLeftLatitude = Double.parseDouble(args[1]);
		System.out.println("You entered : " + upperLeftLatitude);
		System.out.println(" ");
		
		double lowerRightLongitude = Double.parseDouble(args[2]);
		System.out.println("You entered : " + lowerRightLongitude);
		System.out.println(" ");
		
		double lowerRightLatitude = Double.parseDouble(args[3]);
		System.out.println("You entered : " + lowerRightLatitude);
		System.out.println(" ");
		
		
		String BadCurrentDirectory = new String();
		BadCurrentDirectory = args[4];
		CurrentDirectory = BadCurrentDirectory.replace('\\', '/'); 
		System.out.println("tiff files will be saved at :");	
		System.out.println(CurrentDirectory);
		System.out.println(" ");
		
		LatLon upperLeft = LatLon.fromDegrees(upperLeftLatitude, upperLeftLongitude);
		UTMCoord utmUpperLeft = UTMCoord.fromLatLon(upperLeft.latitude, upperLeft.longitude);
		
		LatLon lowerRight = LatLon.fromDegrees(lowerRightLatitude, lowerRightLongitude);
		UTMCoord utmLowerRight = UTMCoord.fromLatLon(lowerRight.latitude, lowerRight.longitude);
		
		double DeltaEst   = utmLowerRight.getEasting() - utmUpperLeft.getEasting();
		double DeltaNorth = utmUpperLeft.getNorthing() - utmLowerRight.getNorthing();
		
		SectorForCrop = new ArrayList<Sector>();
    	
		if(DeltaEst*DeltaNorth >= 100000 )
		{
			System.out.println("Your area is too big for best resolution,");
			System.out.println("creating mosaic tiff with tiles ...");
			System.out.println(" ");
			
			final double constantForBestGSD = 316;
			
			double quotientEstd = DeltaEst/constantForBestGSD;
//			double resteEst = DeltaEst%constantForBestGSD;
			
			double quotientNorthd = DeltaNorth/constantForBestGSD;
//			double resteNorth = DeltaNorth%constantForBestGSD;
			
			int quotientEst = (int) Math.round(quotientEstd);
			int quotientNorth = (int) Math.round(quotientNorthd);
			
			System.out.println("quotientEst = " + quotientEst);
			System.out.println("quotientNorth = " + quotientNorth);
			
			int imgCount = 0;
			for(int n = 0; n < quotientNorth; ++n)
			{
				for (int e = 0; e < quotientEst; ++e)
				{
					double minEst   = utmUpperLeft.getEasting() + constantForBestGSD*e;
					double maxEst   = utmUpperLeft.getEasting() + constantForBestGSD*(e+1);
					
					double minNorth = utmUpperLeft.getNorthing() - constantForBestGSD*(n+1);
					double maxNorth = utmUpperLeft.getNorthing() - constantForBestGSD*n;
				
					SectorForCrop.add(Sector.fromUTMRectangle(utmUpperLeft.getZone(), utmUpperLeft.getHemisphere(), minEst, maxEst, minNorth, maxNorth));
					
					++imgCount;
					System.out.println("Image " + imgCount);
					System.out.println("minEst   = " + minEst);
					System.out.println("maxEst   = " + maxEst);
					System.out.println("minNorth = " + minNorth);
					System.out.println("maxNorth = " + maxNorth);
					System.out.println("n = " + n);
					System.out.println("e = " + e);
					System.out.println(" ");
				}
			}
		}
		else
		{
			SectorForCrop.add(Sector.fromUTMRectangle(utmUpperLeft.getZone(), utmUpperLeft.getHemisphere(), utmUpperLeft.getEasting(), utmLowerRight.getEasting(), utmLowerRight.getNorthing(), utmUpperLeft.getNorthing()));
		}    	
    	
    	// Launch the world Wind template
    	ApplicationTemplate.start("World Wind image download", OrthoExtractorMain.AppFrame.class);
    }

    /**
     * Call the frame developed by NASA World Wind to download the Bing map of the area of interest.
     * @author Geoffrey
     * @see LayerExtractor
     */
	public static class AppFrame extends ApplicationTemplate.AppFrame implements ActionListener
	  {	  
		  private static final long serialVersionUID = 1L;
		  private final WorldWindow wwd;
		  protected LayerTree layerTree;  
		  protected RenderableLayer hiddenLayer; 
		  protected HotSpotController controller;
		  
	      public AppFrame()
	      {
	          //=================================================================================================
	          // 								World-Wind application parameters
	          //=================================================================================================
	    	  super(true, true, false);
	          
	          wwd = this.getWwd();
	          layerTree = new LayerTree();
	          
	          // Specify which World Wind layers to display on screen.
	          List<String> EnableLayers = Arrays.asList("Bing Imagery", "View Controls");
	          LayerList layers = wwd.getModel().getLayers();
	          LayerList currentlayers = new LayerList();
	          for (int i = 0; i < EnableLayers.size(); i++)
	          {
	        	  currentlayers.add(layers.getLayerByName(EnableLayers.get(i)));
	          }
	          
	          // Remove layers which are not needed
	          List<Layer> RemovedLayers = LayerList.getLayersRemoved(layers, currentlayers);
	          for (int i = 0; i < RemovedLayers.size(); i++)
	          {
	        	  layers.remove(RemovedLayers.get(i));
	          }
	          
	          // Enable Bing layer
	          layers.getLayerByName("Bing Imagery").setEnabled(true); 	    
	          
	          // Set up a layer to display the on-screen layer tree in the WorldWindow.
	          hiddenLayer = new RenderableLayer();
	          hiddenLayer.setValue(AVKey.DISPLAY_NAME, "Aerial image outlines");
	          hiddenLayer.addRenderable(this.layerTree);
	          this.getWwd().getModel().getLayers().add(this.hiddenLayer);
	          
	          // Mark the layer as hidden to prevent it being included in the layer tree's model. Including the layer in
	          // the tree would enable the user to hide the layer tree display with no way of bringing it back.
	          hiddenLayer.setValue(AVKey.HIDDEN, true);
	          
	          // Add a controller to handle input events on the layer tree.
	          controller = new HotSpotController(this.getWwd());
	          
	          // Update layer panel
	          this.getLayerPanel().update(this.getWwd());
	          LayerList ActualLayers = this.getWwd().getModel().getLayers();
	          
	          //=================================================================================================
	          // 				Multi-threading for the download of the Bing Ground Images 
	          //=================================================================================================
	          // Creating a loading-bar
	          final loadingBar LoadBarObj = new loadingBar("Wait for downloading ...");
	          LoadBarObj.NumberOfExecutions = SectorForCrop.size();
	          LoadBarObj.numberofUpdate = 1;
	          LoadBarObj.ProgBar.setIndeterminate(true);
	          
	          System.out.println("Downloading " + SectorForCrop.size() + " orthophotos from Bing Map.");
	          
	          // Creating a fix-table which will contain future thread.
	          Thread[] DownloadBingThreadGrid = new Thread[SectorForCrop.size()];
	          
	          // Initializing each separated Thread for downloading image.
	          for(int i = 0; i < SectorForCrop.size(); i++)
	          {
	        	  String PathForDownloadedGroundImage = CurrentDirectory + "\\" + i + ".tif";
	        	  BingImgDownloader ObjForSaveTiffOnDisc = new BingImgDownloader(SectorForCrop.get(i), ActualLayers);
	        	  DownloadBingThreadGrid[i] = new Thread(new ThreadBingImgExtract(PathForDownloadedGroundImage, ObjForSaveTiffOnDisc, LoadBarObj));
	          }
	          
	    	  // Creating a object from ExecutorService type,allowing you to create exactly the same number of Thread than your needed download.
	    	  ExecutorService ThreadForYourCPU = Executors.newFixedThreadPool(SectorForCrop.size());
	          
	          // Launching of each separated thread.
	          for (int i = 0 ; i < SectorForCrop.size(); i++) 
	          {
	        	  ThreadForYourCPU.execute(DownloadBingThreadGrid[i]);
	          }
	          
	          // Waiting for the end of each thread.
	          ThreadForYourCPU.shutdown();
			  try {ThreadForYourCPU.awaitTermination(1, TimeUnit.HOURS);}
			  catch (InterruptedException e) {e.printStackTrace();}
			  
			  // Close the waiting bar.
			  LoadBarObj.LoadBarFrame.dispose();
	          //=================================================================================================
//			  this.dispose();
			  Timer t = new Timer(100,this);
			  t.setRepeats(false);
			  t.start();
	      }

		@Override
		public void actionPerformed(ActionEvent e) 
		{
			this.dispose();
		}
	  }
}

