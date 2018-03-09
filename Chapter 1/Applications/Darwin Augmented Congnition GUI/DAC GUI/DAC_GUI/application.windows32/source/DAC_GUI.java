import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 
import ddf.minim.analysis.*; 
import ddf.minim.ugens.*; 
import java.lang.Math; 
import processing.core.PApplet; 
import java.util.*; 
import java.util.Map.Entry; 
import processing.serial.*; 
import java.awt.event.*; 
import netP5.*; 
import oscP5.*; 
import hypermedia.net.*; 
import processing.net.*; 
import grafica.*; 
import java.lang.reflect.*; 
import java.io.InputStreamReader; 
import java.awt.MouseInfo; 
import java.lang.Process; 
import java.util.Random; 
import java.awt.Robot; 
import java.awt.AWTException; 
import gifAnimation.*; 
import controlP5.*; 
import java.text.DateFormat; 
import java.text.SimpleDateFormat; 
import java.io.OutputStream; 
import ddf.minim.analysis.*; 
import java.io.OutputStream; 
import java.awt.Desktop; 
import java.net.*; 

import edu.ucsd.sccn.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class DAC_GUI extends PApplet {


///////////////////////////////////////////////////////////////////////////////
//
//   GUI for controlling the ADS1299-based OpenBCI
//
//   Created: Chip Audette, Oct 2013 - May 2014
//   Modified: Conor Russomanno & Joel Murphy, August 2014 - Dec 2014
//   Modified (v2.0): Conor Russomanno & Joel Murphy (AJ Keller helped too), June 2016
//
//   Requires gwoptics graphing library for processing.  Built on V0.5.0
//   http://www.gwoptics.org/processing/gwoptics_p5lib/
//
//   Requires ControlP5 library, but an older one.  This will only work
//   with the ControlP5 library that is included with this GitHub repository
//
//   No warranty. Use at your own risk. Use for whatever you'd like.
//
////////////////////////////////////////////////////////////////////////////////

  // To make sound.  Following minim example "frequencyModulation"

 // To make sound.  Following minim example "frequencyModulation"
 //for exp, log, sqrt...they seem better than Processing's built-in

 //for Array.copyOfRange()

 //for serial communication to Arduino/OpenBCI
 //to allow for event listener on screen resize
 //for OSC networking
 //for OSC networking
 //for UDP networking
 // For TCP networking

 // For callbacks
 // For input


// import java.net.InetAddress; // Used for ping, however not working right now.

 //used for simulating mouse clicks




// --------------------------- voice player ------------------------------

Minim soundMinim;
AudioPlayer voice;
BeatDetect beat;
BeatListener bl;
float kickSize, snareSize, hatSize;
boolean watchBeat = false;

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------



String appName = "Augmented Cognition";
String updatedDate = "03/10/2017";
boolean code_debugging = true;

public String Marker_Trigger = "Marker"; //default to none of the options

//boolean code_debugging = false;

//used to switch between application states

final int SYSTEMMODE_INTROANIMATION = -10;
final int SYSTEMMODE_PREINIT = 0;
final int SYSTEMMODE_MIDINIT = 5;
final int SYSTEMMODE_POSTINIT = 10;
int systemMode = SYSTEMMODE_INTROANIMATION; /* Modes: -10 = intro sequence; 0 = system stopped/control panel setings; 10 = gui; 20 = help guide */

boolean midInit = false;
boolean abandonInit = false;

final int NCHAN_CYTON = 8;
final int NCHAN_CYTON_DAISY = 16;
final int NCHAN_GANGLION = 4;

boolean hasIntroAnimation = true;
PImage cog;
PImage darwinLogo;
Gif loadingGIF;
Gif loadingGIF_blue;

//choose where to get the EEG data
final int DATASOURCE_NORMAL_W_AUX = 0; // new default, data from serial with Accel data CHIP 2014-11-03
final int DATASOURCE_GANGLION = 1;  //looking for signal from OpenBCI board via Serial/COM port, no Aux data
final int DATASOURCE_PLAYBACKFILE = 2;  //playback from a pre-recorded text file
final int DATASOURCE_SYNTHETIC = 3;  //Synthetically generated data
public int eegDataSource = -1; //default to none of the options

//here are variables that are used if loading input data from a CSV text file...double slash ("\\") is necessary to make a single slash
String playbackData_fname = "N/A"; //only used if loading input data from a file
// String playbackData_fname;  //leave blank to cause an "Open File" dialog box to appear at startup.  USEFUL!
float playback_speed_fac = 1.0f;  //make 1.0 for real-time.  larger for faster playback
int currentTableRowIndex = 0;
Table_CSV playbackData_table;
int nextPlayback_millis = -100; //any negative number

//Global Serial/COM communications constants
OpenBCI_ADS1299 openBCI = new OpenBCI_ADS1299(); //dummy creation to get access to constants, create real one later
String openBCI_portName = "N/A";  //starts as N/A but is selected from control panel to match your OpenBCI USB Dongle's serial/COM
int openBCI_baud = 115200; //baud rate from the Arduino

OpenBCI_Ganglion ganglion; //dummy creation to get access to constants, create real one later
String ganglion_portName = "N/A";

////// ---- Define variables related to OpenBCI board operations
//Define number of channels from openBCI...first EEG channels, then aux channels
int nchan = NCHAN_CYTON; //Normally, 8 or 16.  Choose a smaller number to show fewer on the GUI
int n_aux_ifEnabled = 3;  // this is the accelerometer data CHIP 2014-11-03
//define variables related to warnings to the user about whether the EEG data is nearly railed (and, therefore, of dubious quality)
DataStatus is_railed[];
final int threshold_railed = PApplet.parseInt(pow(2, 23)-1000);  //fully railed should be +/- 2^23, so set this threshold close to that value
final int threshold_railed_warn = PApplet.parseInt(pow(2, 23)*0.9f); //set a somewhat smaller value as the warning threshold
//OpenBCI SD Card setting (if eegDataSource == 0)
int sdSetting = 0; //0 = do not write; 1 = 5 min; 2 = 15 min; 3 = 30 min; etc...
String sdSettingString = "Do not write to SD";
//openBCI data packet
final int nDataBackBuff = 3*(int)get_fs_Hz_safe();
DataPacket_ADS1299 dataPacketBuff[] = new DataPacket_ADS1299[nDataBackBuff]; //allocate the array, but doesn't call constructor.  Still need to call the constructor!
int curDataPacketInd = -1;
int curBDFDataPacketInd = -1;
int lastReadDataPacketInd = -1;
//related to sync'ing communiction to OpenBCI hardware?
boolean currentlySyncing = false;
long timeOfLastCommand = 0;
////// ---- End variables related to the OpenBCI boards

// define some timing variables for this program's operation
long timeOfLastFrame = 0;
int newPacketCounter = 0;
long timeOfInit;
long timeSinceStopRunning = 1000;
int prev_time_millis = 0;

// final int nPointsPerUpdate = 50; //update the GUI after this many data points have been received
// final int nPointsPerUpdate = 24; //update the GUI after this many data points have been received
final int nPointsPerUpdate = 10; //update the GUI after this many data points have been received


//define some data fields for handling data here in processing
float dataBuffX[];  //define the size later
float dataBuffY_uV[][]; //2D array to handle multiple data channels, each row is a new channel so that dataBuffY[3][] is channel 4
float dataBuffY_filtY_uV[][];
float yLittleBuff[] = new float[nPointsPerUpdate];
float yLittleBuff_uV[][] = new float[nchan][nPointsPerUpdate]; //small buffer used to send data to the filters
float accelerometerBuff[][]; // accelerometer buff 500 points
float auxBuff[][] = new float[3][nPointsPerUpdate];
float data_elec_imp_ohm[];

float displayTime_sec = 5f;    //define how much time is shown on the time-domain montage plot (and how much is used in the FFT plot?)
float dataBuff_len_sec = displayTime_sec + 3f; //needs to be wider than actual display so that filter startup is hidden

//variables for writing EEG data out to a file
OutputFile_rawtxt fileoutput_odf;
OutputFile_BDF fileoutput_bdf;
String output_fname;
String fileName = "N/A";
final int OUTPUT_SOURCE_NONE = 0;
final int OUTPUT_SOURCE_ODF = 1; // The OpenBCI CSV Data Format
final int OUTPUT_SOURCE_BDF = 2; // The BDF data format http://www.biosemi.com/faq/file_format.htm
public int outputDataSource = OUTPUT_SOURCE_ODF;
// public int outputDataSource = OUTPUT_SOURCE_BDF;

//variables for Networking
int port = 0;
String ip = "";
String address = "";
String data_stream = "";
String aux_stream = "";

UDPSend udp;
OSCSend osc;
LSLSend lsl;

// Serial output
String serial_output_portName = "/dev/tty.usbmodem1411";  //must edit this based on the name of the serial/COM port
Serial serial_output;
int serial_output_baud = 115200; //baud rate from the Arduino

//Control Panel for (re)configuring system settings
PlotFontInfo fontInfo;

//program constants
boolean isRunning = false;
boolean redrawScreenNow = true;
int openBCI_byteCount = 0;
byte inByte = -1;    // Incoming serial data
StringBuilder board_message;
StringBuilder scanning_message;

int dollaBillz;
boolean isGettingPoll = false;
boolean spaceFound = false;
boolean scanningChannels = false;
int hexToInt = 0;
boolean dev = false;

//for screen resizing
boolean screenHasBeenResized = false;
float timeOfLastScreenResize = 0;
float timeOfGUIreinitialize = 0;
int reinitializeGUIdelay = 125;
//Tao's variabiles
int widthOfLastScreen = 0;
int heightOfLastScreen = 0;

//set window size
int win_x = 1024;  //window width
int win_y = 768; //window height

PImage logo_blue;
PImage logo_white;
PImage  speakericon;

PFont f1;
PFont f2;
PFont f3;
PFont f4;
PFont f5;

PFont h1; //large Montserrat
PFont h2; //large/medium Montserrat
PFont h3; //medium Montserrat
PFont h4; //small/medium Montserrat
PFont h5; //small Montserrat

PFont p1; //large Open Sans
PFont p2; //large/medium Open Sans
PFont p3; //medium Open Sans
PFont p15;
PFont p4; //medium/small Open Sans
PFont p13;
PFont p5; //small Open Sans
PFont p6; //small Open Sans

ButtonHelpText buttonHelpText;

//EMG_Widget emg_widget;
PulseSensor_Widget pulseWidget;

boolean no_start_connection = false;
boolean has_processed = false;
boolean isOldData = false;

int indices = 0;

boolean synthesizeData = false;

int timeOfSetup = 0;
boolean isHubInitialized = false;
boolean isGanglionObjectInitialized = false;
int bgColor = color(1, 18, 41);
int openbciBlue = color(31, 69, 110);
int COLOR_SCHEME_DEFAULT = 1;
int COLOR_SCHEME_ALTERNATIVE_A = 2;
// int COLOR_SCHEME_ALTERNATIVE_B = 3;
int colorScheme = COLOR_SCHEME_ALTERNATIVE_A;

Process nodeHubby;
int hubPid = 0;
String nodeHubName = "GanglionHub";
Robot rob3115;



//--------------------------- Sound Files ---------------------------------



//-----------------------------------------1-------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//========================SETUP============================//
public void settings(){
  size(1024, 768, P2D);
  PJOGL.setIcon("icon-64.png"); 
}


public void setup() {
 PImage titlebaricon = loadImage("icon-32.png");
  darwinLogo = loadImage("darwin-horizontal-blue2.png");
 
   soundMinim = new Minim(this);
  // this loads mysong.wav from the data folder
  //voice = soundMinim.loadFile(dataPath("m_yes.mp3"));
  //voice.play();
  
  // Step 1: Prepare the exit handler that will attempt to close a running node
  //  server on shut down of this app, the main process.
  // prepareExitHandler();
  if (dev == false) {
    // On windows wait to start the hub until Ganglion is clicked on in the control panel.
    //  See issue #111
    hubStop(); //kill any existing hubs before starting a new one..
    if (!isWindows()) {
      hubInit();
    }
  }

  println("Welcome to the Processing-based Darwin Augmented Cognition GUI!"); //Welcome line.
  println("Last update: " + updatedDate); //Welcome line.
  //println("For more information about how to work with this code base, please visit: http://docs.openbci.com/OpenBCI%20Software/");
  //open window
  //size(1024, 768, P2D);
  
  
  frameRate(60); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
   //turn this off if it's too slow

  surface.setResizable(true);  //updated from frame.setResizable in Processing 2
  widthOfLastScreen = width; //for screen resizing (Thank's Tao)
  heightOfLastScreen = height;

  setupContainers();

  //V1 FONTS
  f1 = createFont("fonts/Raleway-SemiBold.otf", 16);
  f2 = createFont("fonts/Raleway-Regular.otf", 15);
  f3 = createFont("fonts/Raleway-SemiBold.otf", 15);
  f4 = createFont("fonts/Raleway-SemiBold.otf", 64);  // clear bigger fonts for widgets
  f5 = createFont("fonts/Poppins-SemiBold.ttf", 15);  // clear bigger fonts for widgets
  
  h1 = createFont("fonts/Montserrat-Regular.otf", 20);
  h2 = createFont("fonts/Montserrat-Regular.otf", 18);
  h3 = createFont("fonts/Montserrat-Regular.otf", 16);
  h4 = createFont("fonts/Montserrat-Regular.otf", 14);
  h5 = createFont("fonts/Montserrat-Regular.otf", 12);

  p1 = createFont("fonts/OpenSans-Regular.ttf", 20);
  p2 = createFont("fonts/OpenSans-Regular.ttf", 18);
  p3 = createFont("fonts/OpenSans-Regular.ttf", 16);
  p15 = createFont("fonts/OpenSans-Regular.ttf", 15);
  p4 = createFont("fonts/OpenSans-Regular.ttf", 14);
  p13 = createFont("fonts/OpenSans-Regular.ttf", 13);
  p5 = createFont("fonts/OpenSans-Regular.ttf", 12);
  p6 = createFont("fonts/OpenSans-Regular.ttf", 10);

  //listen for window resize ... used to adjust elements in application
  frame.addComponentListener(new ComponentAdapter() {
    public void componentResized(ComponentEvent e) {
      if (e.getSource()==frame) {
        println(appName + ": setup: RESIZED");
        screenHasBeenResized = true;
        timeOfLastScreenResize = millis();
        // initializeGUI();
      }
    }
  }
  );

  fontInfo = new PlotFontInfo();
  helpWidget = new HelpWidget(0, win_y - 30, win_x, 30);

  //setup topNav
  topNav = new TopNav();

  //from the user's perspective, the program hangs out on the ControlPanel until the user presses "Start System".
  print("Graphics & GUI Library: ");
  controlPanel = new ControlPanel(this);
  //The effect of "Start System" is that initSystem() gets called, which starts up the conneciton to the OpenBCI
  //hardware (via the "updateSyncState()" process) as well as initializing the rest of the GUI elements.
  //Once the hardware is synchronized, the main GUI is drawn and the user switches over to the main GUI.

  logo_blue = loadImage("logo_blue.png");
  logo_white = loadImage("darwin-logo-white.png");
  cog = loadImage("darwin-logo-blue.png");
  loadingGIF = new Gif(this, "OpenBCI-LoadingGIF-2.gif");
  loadingGIF.loop();
  loadingGIF_blue = new Gif(this, "OpenBCI-LoadingGIF-blue-256.gif");
  loadingGIF_blue.loop();

  playground = new Playground(navBarHeight);

  //attempt to open a serial port for "output"
  try {
    verbosePrint(appName + ":: attempting to open serial/COM port for data output = " + serial_output_portName);
    serial_output = new Serial(this, serial_output_portName, serial_output_baud); //open the com port
    serial_output.clear(); // clear anything in the com port's buffer
  }
  catch (RuntimeException e) {
    verbosePrint(appName + ":: could not open " + serial_output_portName);
  }

  // println(appName + ": setup: hub is running " + ganglion.isHubRunning());
  buttonHelpText = new ButtonHelpText();

  //myPresentation = new Presentation();

  // try{
  //   rob3115 = new Robot();
  // } catch (AWTException e){
  //   println("couldn't create robot...");
  // }

  // ganglion = new OpenBCI_Ganglion(this);
  // wm = new WidgetManager(this);

  timeOfSetup = millis(); //keep track of time when setup is finished... used to make sure enough time has passed before creating some other objects (such as the Ganglion instance)
  //PImage icon = loadImage("darwin-icon.png");
  //surface.setIcon(icon);
  
}
//====================== END-OF-SETUP ==========================//

//======================== DRAW LOOP =============================//

public void draw() {
  drawLoop_counter++; //signPost("10");
  systemUpdate(); //signPost("20");
  systemDraw();   //signPost("30");
  
}

//====================== END-OF-DRAW ==========================//

/**
 * This allows us to kill the running node process on quit.
 */
private void prepareExitHandler () {
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    public void run () {
      System.out.println("SHUTDOWN HOOK");
      try {
        if (hubStop()) {
          System.out.println("SHUTDOWN HUB");
        } else {
          System.out.println("FAILED TO SHUTDOWN HUB");
        }
      }
      catch (Exception ex) {
        ex.printStackTrace(); // not much else to do at this point
      }
    }
  }
  ));
}

/**
 * Starts the hub and sets prepares the exit handler.
 */
public void hubInit() {
  isHubInitialized = true;
  hubStart();
  prepareExitHandler();
}

/**
 * Starts the node hub working, tested on mac and windows.
 */
public void hubStart() {
  println("Launching application from local data dir");
  try {
    // https://forum.processing.org/two/discussion/13053/use-launch-for-applications-kept-in-data-folder
    if (isWindows()) {
      println(appName + ": hubStart: OS Detected: Windows");
      nodeHubby = launch(dataPath("GanglionHub.exe"));
    } else if (isLinux()) {
      println(appName + ": hubStart: OS Detected: Linux");
      nodeHubby = exec(dataPath("GanglionHub"));
    } else {
      println(appName + ": hubStart: OS Detected: Mac");
      nodeHubby = launch(dataPath("GanglionHub.app"));
    }
    // hubRunning = true;
  }
  catch (Exception e) {
    println("hubStart: " + e);
  }
}

/**
 * @description Single function to call at the termination program hook.
 */
public boolean hubStop() {
  if (isWindows()) {
    return killRunningprocessWin();
  } else {
    killRunningProcessMac();
    return true;
  }
}

/**
 * @description Helper function to determine if the system is linux or not.
 * @return {boolean} true if os is linux, false otherwise.
 */
private boolean isLinux() {
  return System.getProperty("os.name").toLowerCase().indexOf("linux") > -1;
}

/**
 * @description Helper function to determine if the system is windows or not.
 * @return {boolean} true if os is windows, false otherwise.
 */
private boolean isWindows() {
  return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;
}

/**
 * @description Parses the running process list for processes whose name have ganglion hub, if found, kills them one by one.
 *  function dubbed "death dealer"
 */
public void killRunningProcessMac() {
  try {
    String line;
    Process p = Runtime.getRuntime().exec("ps -e");
    BufferedReader input =
      new BufferedReader(new InputStreamReader(p.getInputStream()));
    while ((line = input.readLine()) != null) {
      if (line.contains(nodeHubName)) {
        try {
          endProcess(getProcessIdFromLineMac(line));
          println("Killed: " + line);
        }
        catch (Exception err) {
          println("Failed to stop process: " + line + "\n\n");
          err.printStackTrace();
        }
      }
    }
    input.close();
  }
  catch (Exception err) {
    err.printStackTrace();
  }
}

/**
 * @description Parses the running process list for processes whose name have ganglion hub, if found, kills them one by one.
 *  function dubbed "death dealer" aka "cat killer"
 */
public boolean killRunningprocessWin() {
  try {
    Runtime.getRuntime().exec("taskkill /F /IM GanglionHub.exe");
    return true;
  }
  catch (Exception err) {
    err.printStackTrace();
    return false;
  }
}

/**
 * @description Parses a mac process line and grabs the pid, the first component.
 * @return {int} the process id
 */
public int getProcessIdFromLineMac(String line) {
  line = trim(line);
  String[] components = line.split(" ");
  return Integer.parseInt(components[0]);
}

public void endProcess(int pid) {
  Runtime rt = Runtime.getRuntime();
  try {
    rt.exec("kill -9 " + pid);
  }
  catch (IOException err) {
    err.printStackTrace();
  }
}

int pointCounter = 0;
int prevBytes = 0;
int prevMillis = millis();
int byteRate_perSec = 0;
int drawLoop_counter = 0;

//used to init system based on initial settings...Called from the "Start System" button in the GUI's ControlPanel

public void setupWidgetManager() {
  wm = new WidgetManager(this);
}

public void initSystem() {

  println();
  println();
  println("=================================================");
  println("||             INITIALIZING SYSTEM             ||");
  println("=================================================");
  println();

  verbosePrint(appName + ": initSystem: -- Init 0 -- " + millis());
  timeOfInit = millis(); //store this for timeout in case init takes too long
  verbosePrint("timeOfInit = " + timeOfInit);

  //prepare data variables
  verbosePrint(appName + ": initSystem: Preparing data variables...");
  dataBuffX = new float[(int)(dataBuff_len_sec * get_fs_Hz_safe())];
  dataBuffY_uV = new float[nchan][dataBuffX.length];
  dataBuffY_filtY_uV = new float[nchan][dataBuffX.length];
  accelerometerBuff = new float[3][500]; // 500 points
  for (int i=0; i<n_aux_ifEnabled; i++) {
    for (int j=0; j<accelerometerBuff[0].length; j++) {
      accelerometerBuff[i][j] = 0;
    }
  }
  //data_std_uV = new float[nchan];
  data_elec_imp_ohm = new float[nchan];
  is_railed = new DataStatus[nchan];
  for (int i=0; i<nchan; i++) is_railed[i] = new DataStatus(threshold_railed, threshold_railed_warn);
  for (int i=0; i<nDataBackBuff; i++) {
    dataPacketBuff[i] = new DataPacket_ADS1299(nchan, n_aux_ifEnabled);
  }
  dataProcessing = new DataProcessing(nchan, get_fs_Hz_safe());
  dataProcessing_user = new DataProcessing_User(nchan, get_fs_Hz_safe());



  //initialize the data
  prepareData(dataBuffX, dataBuffY_uV, get_fs_Hz_safe());

  verbosePrint(appName + ": initSystem: -- Init 1 -- " + millis());

  //initialize the FFT objects
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    verbosePrint("Init FFT Buff \u2013 "+Ichan);
    fftBuff[Ichan] = new FFT(Nfft, get_fs_Hz_safe());
  }  //make the FFT objects

  initializeFFTObjects(fftBuff, dataBuffY_uV, Nfft, get_fs_Hz_safe());

  //prepare some signal processing stuff
  //for (int Ichan=0; Ichan < nchan; Ichan++) { detData_freqDomain[Ichan] = new DetectionData_FreqDomain(); }

  verbosePrint(appName + ": initSystem: -- Init 2 -- " + millis());

  //prepare the source of the input data
  switch (eegDataSource) {
  case DATASOURCE_NORMAL_W_AUX:
    int nEEDataValuesPerPacket = nchan;
    boolean useAux = false;
    if (eegDataSource == DATASOURCE_NORMAL_W_AUX) useAux = true;  //switch this back to true CHIP 2014-11-04
    openBCI = new OpenBCI_ADS1299(this, openBCI_portName, openBCI_baud, nEEDataValuesPerPacket, useAux, n_aux_ifEnabled); //this also starts the data transfer after XX seconds
    break;
  case DATASOURCE_SYNTHETIC:
    //do nothing
    break;
  case DATASOURCE_PLAYBACKFILE:
    //open and load the data file
    println(appName + ": initSystem: loading playback data from " + playbackData_fname);
    try {
      playbackData_table = new Table_CSV(playbackData_fname);
    }
    catch (Exception e) {
      println(appName + ": initSystem: could not open file for playback: " + playbackData_fname);
      println("   : quitting...");
      exit();
    }
    println(appName + ": initSystem: loading complete.  " + playbackData_table.getRowCount() + " rows of data, which is " + round(PApplet.parseFloat(playbackData_table.getRowCount())/get_fs_Hz_safe()) + " seconds of EEG data");
    //removing first column of data from data file...the first column is a time index and not eeg data
    playbackData_table.removeColumn(0);
    break;
  case DATASOURCE_GANGLION:
    ganglion.connectBLE(ganglion_portName);
    break;
  default:
    break;
  }

  verbosePrint(appName + ": initSystem: -- Init 3 -- " + millis());

  if (abandonInit) {
    haltSystem();
    println("Failed to connect to data source...");
    output("Failed to connect to data source...");
  } else {
    println("  3a -- " + millis());
    //initilize the GUI
    // initializeGUI(); //will soon be destroyed... and replaced with ...  wm = new WidgetManager(this);
    topNav.initSecondaryNav();
    println("  3b -- " + millis());

    // wm = new WidgetManager(this);
    setupWidgetManager();

    if (!abandonInit) {
      println("  3c -- " + millis());
      // setupGUIWidgets(); //####

      //open data file
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) openNewLogFile(fileName);  //open a new log file
      if (eegDataSource == DATASOURCE_GANGLION) openNewLogFile(fileName); // println("open ganglion output file");

      nextPlayback_millis = millis(); //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.

      if (eegDataSource != DATASOURCE_GANGLION && eegDataSource != DATASOURCE_NORMAL_W_AUX) {
        systemMode = SYSTEMMODE_POSTINIT; //tell system it's ok to leave control panel and start interfacing GUI
      }
      if (!abandonInit) {
        println("WOOHOO!!!");
        controlPanel.close();
      } else {
        haltSystem();
        println("Failed to connect to data source...");
        output("Failed to connect to data source...");
      }
    } else {
      haltSystem();
      println("Failed to connect to data source...");
      output("Failed to connect to data source...");
    }
  }

  verbosePrint(appName + ": initSystem: -- Init 4 -- " + millis());

  //reset init variables
  midInit = false;
  abandonInit = false;
}

/**
 * @description Useful function to get the correct sample rate based on data source
 * @returns `float` - The frequency / sample rate of the data source
 */
public float get_fs_Hz_safe() {
  if (eegDataSource == DATASOURCE_GANGLION) {
    return ganglion.get_fs_Hz();
  } else {
    return openBCI.get_fs_Hz();
  }
}

//halt the data collection
public void haltSystem() {
  println(appName + ": haltSystem: Halting system for reconfiguration of settings...");
  if (initSystemButton.but_txt == "STOP SYSTEM") {
    initSystemButton.but_txt = "START SYSTEM";
  }
  stopRunning();  //stop data transfer

  //reset variables for data processing
  curDataPacketInd = -1;
  lastReadDataPacketInd = -1;
  pointCounter = 0;
  currentTableRowIndex = 0;
  prevBytes = 0;
  prevMillis = millis();
  byteRate_perSec = 0;
  drawLoop_counter = 0;
  // eegDataSource = -1;
  //set all data source list items inactive

  //reset connect loadStrings
  openBCI_portName = "N/A";  // Fixes inability to reconnect after halding  JAM 1/2017
  ganglion_portName = "";
  controlPanel.resetListItems();

  // stopDataTransfer(); // make sure to stop data transfer, if data is streaming and being drawn

  if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
    closeLogFile();  //close log file
    openBCI.closeSDandSerialPort();
  }
  if (eegDataSource == DATASOURCE_GANGLION) {
    closeLogFile();  //close log file
    ganglion.disconnectBLE();
  }
  systemMode = SYSTEMMODE_PREINIT;
}

public void systemUpdate() { // for updating data values and variables

  if (isHubInitialized && isGanglionObjectInitialized == false && millis() - timeOfSetup >= 1500) {
    ganglion = new OpenBCI_Ganglion(this);
    println("Instantiating Ganglion object...");
    isGanglionObjectInitialized = true;
  }

  //update the sync state with the OpenBCI hardware
  if (openBCI.state == openBCI.STATE_NOCOM || openBCI.state == openBCI.STATE_COMINIT || openBCI.state == openBCI.STATE_SYNCWITHHARDWARE) {
    openBCI.updateSyncState(sdSetting);
  }

  //prepare for updating the GUI
  win_x = width;
  win_y = height;


  if (systemMode == SYSTEMMODE_PREINIT) {
    //updates while in system control panel before START SYSTEM
    controlPanel.update();
    topNav.update();

    if (widthOfLastScreen != width || heightOfLastScreen != height) {
      topNav.screenHasBeenResized(width, height);
    }
  }
  if (systemMode == SYSTEMMODE_POSTINIT) {
    if (isRunning) {
      //get the data, if it is available
      pointCounter = getDataIfAvailable(pointCounter);

      //has enough data arrived to process it and update the GUI?
      if (pointCounter >= nPointsPerUpdate) {
        pointCounter = 0;  //reset for next time

        //process the data
        processNewData();
        //Matrix_update();
        if ((millis() - timeOfGUIreinitialize) > reinitializeGUIdelay) { //wait 1 second for GUI to reinitialize
          try {

            //-----------------------------------------------------------
            //-----------------------------------------------------------
            // gui.update(dataProcessing.data_std_uV, data_elec_imp_ohm);
            // topNav.update();
            // updateGUIWidgets(); //####
            //-----------------------------------------------------------
            //-----------------------------------------------------------
          }
          catch (Exception e) {
            println(e.getMessage());
            reinitializeGUIdelay = reinitializeGUIdelay * 2;
            println(appName + ": systemUpdate: New GUI reinitialize delay = " + reinitializeGUIdelay);
          }
        } else {
          println(appName + ": systemUpdate: reinitializing GUI after resize... not updating GUI");
        }

        redrawScreenNow=true;
      } else {
        //not enough data has arrived yet... only update the channel controller
      }
    } else if (eegDataSource == DATASOURCE_PLAYBACKFILE && !has_processed && !isOldData) {
      lastReadDataPacketInd = 0;
      pointCounter = 0;
      try {
        process_input_file();
      }
      catch(Exception e) {
        isOldData = true;
        output("Error processing timestamps, are you using old data?");
      }
    }

    // gui.cc.update(); //update Channel Controller even when not updating certain parts of the GUI... (this is a bit messy...)

    //alternative component listener function (line 177 - 187 frame.addComponentListener) for processing 3,
    if (widthOfLastScreen != width || heightOfLastScreen != height) {
      println(appName + ": setup: RESIZED");
      screenHasBeenResized = true;
      timeOfLastScreenResize = millis();
      widthOfLastScreen = width;
      heightOfLastScreen = height;
    }

    //re-initialize GUI if screen has been resized and it's been more than 1/2 seccond (to prevent reinitialization of GUI from happening too often)
    if (screenHasBeenResized) {
      // GUIWidgets_screenResized(width, height);
      topNav.screenHasBeenResized(width, height);
      wm.screenResized();
    }
    if (screenHasBeenResized == true && (millis() - timeOfLastScreenResize) > reinitializeGUIdelay) {
      screenHasBeenResized = false;
      println("systemUpdate: reinitializing GUI");
      timeOfGUIreinitialize = millis();
      // initializeGUI();
      // GUIWidgets_screenResized(width, height);
      playground.x = width; //reset the x for the playground...
    }

    wm.update();
    playground.update();
  }
}

public void systemDraw() { //for drawing to the screen

  //redraw the screen...not every time, get paced by when data is being plotted
  background(bgColor);  //clear the screen
  noStroke();
  //background(255);  //clear the screen

  if (systemMode >= SYSTEMMODE_POSTINIT) {
    int drawLoopCounter_thresh = 100;
    if ((redrawScreenNow) || (drawLoop_counter >= drawLoopCounter_thresh)) {
      //if (drawLoop_counter >= drawLoopCounter_thresh) println(appName + ": redrawing based on loop counter...");
      drawLoop_counter=0; //reset for next time
      redrawScreenNow = false;  //reset for next time

      //update the title of the figure;
      switch (eegDataSource) {
      case DATASOURCE_NORMAL_W_AUX:
        switch (outputDataSource) {
        case OUTPUT_SOURCE_ODF:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps, Byte Count = " + openBCI_byteCount + ", bit rate = " + byteRate_perSec*8 + " bps" + ", " + PApplet.parseInt(PApplet.parseFloat(fileoutput_odf.getRowsWritten())/get_fs_Hz_safe()) + " secs Saved, Writing to " + output_fname);
          break;
        case OUTPUT_SOURCE_BDF:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps, Byte Count = " + openBCI_byteCount + ", bit rate = " + byteRate_perSec*8 + " bps" + ", " + PApplet.parseInt(fileoutput_bdf.getRecordsWritten()) + " secs Saved, Writing to " + output_fname);
          break;
        case OUTPUT_SOURCE_NONE:
        default:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps, Byte Count = " + openBCI_byteCount + ", bit rate = " + byteRate_perSec*8 + " bps");
          break;
        }
        break;
      case DATASOURCE_SYNTHETIC:
        surface.setTitle(PApplet.parseInt(frameRate) + " fps, Using Synthetic EEG Data");
        break;
      case DATASOURCE_PLAYBACKFILE:
        surface.setTitle(PApplet.parseInt(frameRate) + " fps, Playing " + PApplet.parseInt(PApplet.parseFloat(currentTableRowIndex)/get_fs_Hz_safe()) + " of " + PApplet.parseInt(PApplet.parseFloat(playbackData_table.getRowCount())/get_fs_Hz_safe()) + " secs, Reading from: " + playbackData_fname);
        break;
      case DATASOURCE_GANGLION:
        surface.setTitle(PApplet.parseInt(frameRate) + " fps, Ganglion!");
        break;
      }
    }

    //wait 1 second for GUI to reinitialize
    if ((millis() - timeOfGUIreinitialize) > reinitializeGUIdelay) {
      // println("attempting to draw GUI...");
      try {
        // println("GUI DRAW!!! " + millis());

        //----------------------------
        // gui.draw(); //draw the GUI

        wm.draw();
        //updateGUIWidgets(); //####
        // drawGUIWidgets();

        // topNav.draw();

        //----------------------------

        // playground.draw();
      }
      catch (Exception e) {
        println(e.getMessage());
        reinitializeGUIdelay = reinitializeGUIdelay * 2;
        println(appName + ": systemDraw: New GUI reinitialize delay = " + reinitializeGUIdelay);
      }
    } else {
      //reinitializing GUI after resize
      println(appName + ": systemDraw: reinitializing GUI after resize... not drawing GUI");
    }

    //dataProcessing_user.draw();
    drawContainers();
  } else { //systemMode != 10
    //still print title information about fps
    surface.setTitle(PApplet.parseInt(frameRate) + " fps \u2014 Augmented Cognition GUI");
  }

  if (systemMode >= SYSTEMMODE_PREINIT) {
    topNav.draw();

    //control panel
    if (controlPanel.isOpen) {
      controlPanel.draw();
    }

    helpWidget.draw();
  }


  if (systemMode == SYSTEMMODE_INTROANIMATION) {
    //intro animation sequence
    if (hasIntroAnimation) {
      introAnimation();
    } else {
      systemMode = SYSTEMMODE_PREINIT;
    }
  }


  if ((openBCI.get_state() == openBCI.STATE_COMINIT || openBCI.get_state() == openBCI.STATE_SYNCWITHHARDWARE) && systemMode == SYSTEMMODE_PREINIT) {
    //make out blink the text "Initalizing GUI..."
    pushStyle();
    imageMode(CENTER);
    image(loadingGIF, width/2, height/2, 128, 128);//render loading gif...
    popStyle();
    if (millis()%1000 < 500) {
      output("Attempting to establish a connection with your OpenBCI Board...");
    } else {
      output("");
    }

    if (millis() - timeOfInit > 12000) {
      haltSystem();
      initSystemButton.but_txt = "START SYSTEM";
      output("Init timeout. Verify your Serial/COM Port. Power DOWN/UP your OpenBCI & USB Dongle. Then retry Initialization.");
      controlPanel.open();
    }
  }

  //draw presentation last, bc it is intended to be rendered on top of the GUI ...
  //if (drawPresentation) {
    //myPresentation.draw();
    //emg_widget.drawTriggerFeedback();
    //dataProcessing_user.drawTriggerFeedback();
  //}

  // use commented code below to verify frameRate and check latency
  // println("Time since start: " + millis() + " || Time since last frame: " + str(millis()-timeOfLastFrame));
  // timeOfLastFrame = millis();

  buttonHelpText.draw();
  mouseOutOfBounds(); // to fix
}

public void introAnimation() {
  pushStyle();
  imageMode(CENTER);
  background(255);
  int t1 = 1000;
  int t2 = 2000;
  int t3 = 1000;
  if (code_debugging){
    t1 = 1000;
    t2 = 2000;
    t3 = 1000;
  }else{
    t1 = 2000;
    t2 = 4000;
    t3 = 6000;
  }
  float transparency = 0;

  if (millis() >= t1) {
    transparency = map(millis(), t1, t2, 0, 255);
    tint(255, transparency);
    //draw OpenBCI Logo Front & Center
    image(cog, width/2, height/2, width/3, width/6);
    textFont(p3, 16);
    textLeading(24);
    fill(31, 69, 110, transparency);
    textAlign(CENTER, CENTER);
    //text("OpenBCI GUI v2.1.2\nJanuary 2017", width/2, height/2 + width/9);
    text("Darwin Ecosystem\nAugmented Cognition GUI v2.1.2\nFebruary 2017", width/2, height/2 + width/9);
  }

  //exit intro animation at t2
  if (millis() >= t3) {
    systemMode = SYSTEMMODE_PREINIT;
    controlPanel.isOpen = true;
  }
  popStyle();
}

//CODE FOR FIXING WEIRD EXIT CRASH ISSUE -- 7/27/16 ===========================
boolean mouseInFrame = false;
boolean windowOriginSet = false;
int appletOriginX = 0;
int appletOriginY = 0;
PVector loc;

public void mouseOutOfBounds() {
  if (windowOriginSet && mouseInFrame) {

    try {
      if (MouseInfo.getPointerInfo().getLocation().x <= appletOriginX ||
        MouseInfo.getPointerInfo().getLocation().x >= appletOriginX+width ||
        MouseInfo.getPointerInfo().getLocation().y <= appletOriginY ||
        MouseInfo.getPointerInfo().getLocation().y >= appletOriginY+height) {
        mouseX = 0;
        mouseY = 0;
        // println("Mouse out of bounds!");
        mouseInFrame = false;
      }
    }
    catch (RuntimeException e) {
      verbosePrint("Error happened while cursor left application...");
    }
  } else {
    if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
      loc = getWindowLocation(P2D);
      appletOriginX = (int)loc.x;
      appletOriginY = (int)loc.y;
      windowOriginSet = true;
      mouseInFrame = true;
    }
  }
}

public PVector getWindowLocation(String renderer) {
  PVector l = new PVector();
  if (renderer == P2D || renderer == P3D) {
    com.jogamp.nativewindow.util.Point p = new com.jogamp.nativewindow.util.Point();
    ((com.jogamp.newt.opengl.GLWindow)surface.getNative()).getLocationOnScreen(p);
    l.x = p.getX();
    l.y = p.getY();
  } else if (renderer == JAVA2D) {
    java.awt.Frame f =  (java.awt.Frame) ((processing.awt.PSurfaceAWT.SmoothCanvas) surface.getNative()).getFrame();
    l.x = f.getX();
    l.y = f.getY();
  }
  return l;
}

class BeatListener implements AudioListener
{
  private BeatDetect beat;
  private AudioPlayer source;
  
  BeatListener(BeatDetect beat, AudioPlayer source)
  {
    this.source = source;
    this.source.addListener(this);
    this.beat = beat;
  }
  
  public void samples(float[] samps)
  {
    beat.detect(source.mix);
  }
  
  public void samples(float[] sampsL, float[] sampsR)
  {
    beat.detect(source.mix);
  }
}
//END OF CODE FOR FIXING WEIRD EXIT CRASH ISSUE -- 7/27/16 ===========================
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//   This code is used for GUI-wide spacing. It defines the GUI layout as a grid
//   with the following design:
//
//   The #s shown below fall at the center of their corresponding container[].
//   Ex: container[1] is the upper left corner of the large rectangle between [0] & [10]
//   Ex 2: container[6] is the entire right half of the same rectangle.
//
//   ------------------------------------------------
//   |                      [0]                     |
//   ------------------------------------------------
//   |                       |         [11]         |
//   |         [1]          [2]---[15]--[3]---[16]--|
//   |                       |         [12]         |
//   |---------[4]----------[5]---------[6]---------|
//   |                       |         [13]         |
//   |         [7]          [8]---[17]--[9]---[18]--|
//   |                       |         [14]         |
//   ------------------------------------------------
//   |                      [10]                    |
//   ------------------------------------------------
//
//   Created by: Conor Russomanno (May 2016)
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

boolean drawContainers = false;
Container[] container = new Container[19];

//Viz extends container (example below)
//Viz viz1;
//Viz viz2;

int widthOfLastScreen_C = 0;
int heightOfLastScreen_C = 0;

int topNav_h = 64; //tie this to a global variable or one attached to GUI_Manager
int bottomNav_h = 28; //same
int leftNav_w = 0; //not used currently, maybe if we add a left-side tool bar
int rightNav_w = 0; //not used currently

public void setupContainers() {

  widthOfLastScreen_C = width;
  heightOfLastScreen_C = height;

  container[0] = new Container(0, 0, width, topNav_h, 0);
  container[5] = new Container(0, topNav_h, width, height - (topNav_h + bottomNav_h), 1);
  container[1] = new Container(container[5], "TOP_LEFT");
  container[2] = new Container(container[5], "TOP");
  container[3] = new Container(container[5], "TOP_RIGHT");
  container[4] = new Container(container[5], "LEFT");
  container[6] = new Container(container[5], "RIGHT");
  container[7] = new Container(container[5], "BOTTOM_LEFT");
  container[8] = new Container(container[5], "BOTTOM");
  container[9] = new Container(container[5], "BOTTOM_RIGHT");
  container[10] = new Container(0, height - bottomNav_h, width, 50, 0);
  container[11] = new Container(container[3], "TOP");
  container[12] = new Container(container[3], "BOTTOM");
  container[13] = new Container(container[9], "TOP");
  container[14] = new Container(container[9], "BOTTOM");
  container[15] = new Container(container[6], "TOP_LEFT");
  container[16] = new Container(container[6], "TOP_RIGHT");
  container[17] = new Container(container[6], "BOTTOM_LEFT");
  container[18] = new Container(container[6], "BOTTOM_RIGHT");
  //container11 = new Container(container1, "LEFT");
  //container12 = new Container(container1, "RIGHT");

  //setup viz objects... example of container extension (more below)
  //setupVizs();
}

public void drawContainers() {
  //background(255);
  for(int i = 0; i < container.length; i++){
    container[i].draw();
  }
  //container11.draw();
  //container12.draw();

  //Draw viz objects.. exampl extension of container class (more below)
  //viz1.draw();
  //viz2.draw();

  //alternative component listener function (line 177 - 187 frame.addComponentListener) for processing 3,
  if (widthOfLastScreen_C != width || heightOfLastScreen_C != height) {
    println(appName + ": setup: RESIZED");
    //screenHasBeenResized = true;
    //timeOfLastScreenResize = millis();
    setupContainers();
    //setupVizs(); //container extension example (more below)
    widthOfLastScreen = width;
    heightOfLastScreen = height;
  }
}

public class Container {

  //key Container Variables
  public float x0, y0, w0, h0; //true dimensions.. without margins
  public float x, y, w, h; //dimensions with margins
  public float margin; //margin

  //constructor 1 -- comprehensive
  public Container(float _x0, float _y0, float _w0, float _h0, float _margin) {

    margin = _margin;

    x0 = _x0;
    y0 = _y0;
    w0 = _w0;
    h0 = _h0;

    x = x0 + margin;
    y = y0 + margin;
    w = w0 - margin*2;
    h = h0 - margin*2;
  }

  //constructor 2 -- recursive constructor -- for quickly building sub-containers based on a super container (aka master)
  public Container(Container master, String _type) {

    margin = master.margin;

    if(_type == "WHOLE"){
      x0 = master.x0;
      y0 = master.y0;
      w0 = master.w0;
      h0 = master.h0;
      w = master.w;
      h = master.h;
      x = master.x;
      y = master.y;
    } else if (_type == "LEFT") {
      x0 = master.x0;
      y0 = master.y0;
      w0 = master.w0/2;
      h0 = master.h0;
      w = (master.w - margin)/2;
      h = master.h;
      x = master.x;
      y = master.y;
    } else if (_type == "RIGHT") {
      x0 = master.x0 + master.w0/2;
      y0 = master.y0;
      w0 = master.w0/2;
      h0 = master.h0;
      w = (master.w - margin)/2;
      h = master.h;
      x = master.x + w + margin;
      y = master.y;
    } else if (_type == "TOP") {
      x0 = master.x0;
      y0 = master.y0;
      w0 = master.w0;
      h0 = master.h0/2;
      w = master.w;
      h = (master.h - margin)/2;
      x = master.x;
      y = master.y;
    } else if (_type == "BOTTOM") {
      x0 = master.x0;
      y0 = master.y0 + master.h0/2;
      w0 = master.w0;
      h0 = master.h0/2;
      w = master.w;
      h = (master.h - margin)/2;
      x = master.x;
      y = master.y + h + margin;
    } else if (_type == "TOP_LEFT") {
      x0 = master.x0;
      y0 = master.y0;
      w0 = master.w0/2;
      h0 = master.h0/2;
      w = (master.w - margin)/2;
      h = (master.h - margin)/2;
      x = master.x;
      y = master.y;
    } else if (_type == "TOP_RIGHT") {
      x0 = master.x0 + master.w0/2;
      y0 = master.y0;
      w0 = master.w0/2;
      h0 = master.h0/2;
      w = (master.w - margin)/2;
      h = (master.h - margin)/2;
      x = master.x + w + margin;
      y = master.y;
    } else if (_type == "BOTTOM_LEFT") {
      x0 = master.x0;
      y0 = master.y0 + master.h0/2;
      w0 = master.w0/2;
      h0 = master.h0/2;
      w = (master.w - margin)/2;
      h = (master.h - margin)/2;
      x = master.x;
      y = master.y + h + margin;
    } else if (_type == "BOTTOM_RIGHT") {
      x0 = master.x0 + master.w0/2;
      y0 = master.y0 + master.h0/2;
      w0 = master.w0/2;
      h0 = master.h0/2;
      w = (master.w - margin)/2;
      h = (master.h - margin)/2;
      x = master.x + w + margin;
      y = master.y + h + margin;
    }
  }

  public void draw() {
    if(drawContainers){
      pushStyle();

      //draw margin area
      fill(102, 255, 71, 100);
      noStroke();
      rect(x0, y0, w0, h0);

      //noFill();
      //stroke(255, 0, 0);
      //rect(x0, y0, w0, h0);

      fill(31, 69, 110, 100);
      noStroke();
      rect(x, y, w, h);

      popStyle();
    }
  }
};

// --- EXAMPLE OF EXTENDING THE CONTAINER --- //

//public class Viz extends Container {
//  public float abc;

//  public Viz(float _abc, Container master) {
//    super(master, "WHOLE");
//    abc = _abc;
//  }

//  void draw() {
//    pushStyle();
//    noStroke();
//    fill(255, 0, 0, 50);
//    rect(x, y, w, h);
//    popStyle();
//  }
//};

//void setupVizs() {
//  viz1 = new Viz (10f, container2);
//  viz2 = new Viz (10f, container4);
//}

// --- END OF EXAMPLE OF EXTENDING THE CONTAINER --- //
//////////////////////////////////////////////////////////////////////////
//
//		System Control Panel
//		- Select serial port from dropdown
//		- Select default configuration (EEG, EKG, EMG)
//		- Select Electrode Count (8 vs 16)
//		- Select data mode (synthetic, playback file, real-time)
//		- Record data? (y/n)
//			- select output location
//		- link to help guide
//		- buttons to start/stop/reset application
//
//		Written by: Conor Russomanno (Oct. 2014)
//
//////////////////////////////////////////////////////////////////////////



//------------------------------------------------------------------------
//                       Global Variables  & Instances
//------------------------------------------------------------------------

ControlPanel controlPanel;

ControlP5 cp5; //program-wide instance of ControlP5
ControlP5 cp5Popup;
CallbackListener cb = new CallbackListener() { //used by ControlP5 to clear text field on double-click
  public void controlEvent(CallbackEvent theEvent) {

    if (cp5.isMouseOver(cp5.get(Textfield.class, "fileName"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "fileName").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "fileNameGanglion"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "fileNameGanglion").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "udp_ip"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "udp_ip").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "udp_port"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "udp_port").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "osc_ip"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "osc_ip").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "osc_address"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "osc_address").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "lsl_data"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "lsl_data").clear();
    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "lsl_aux"))){
      println("CallbackListener: controlEvent: clearing");
      cp5.get(Textfield.class, "lsl_aux").clear();
    }
  }
};

MenuList sourceList;

//Global buttons and elements for the control panel (changed within the classes below)
MenuList serialList;
String[] serialPorts = new String[Serial.list().length];

MenuList bleList;

MenuList sdTimes;

MenuList channelList;

MenuList pollList;

int boxColor = color(200);
int boxStrokeColor = color(bgColor);
int isSelected_color = color(184, 220, 105);

// Button openClosePort;
// boolean portButtonPressed;

int networkType = 0;

boolean calledForBLEList = false;

Button refreshPort;
Button refreshBLE;
Button autoconnect;
Button initSystemButton;
Button autoFileName;
Button outputBDF;
Button outputODF;

Button autoFileNameGanglion;
Button outputODFGanglion;
Button outputBDFGanglion;

Button chanButton8;
Button chanButton16;
Button selectPlaybackFile;
Button selectSDFile;
Button popOut;

//Radio Button Definitions
Button getChannel;
Button setChannel;
Button ovrChannel;
// Button getPoll;
// Button setPoll;
// Button defaultBAUD;
// Button highBAUD;
Button autoscan;
// Button autoconnectNoStartDefault;
// Button autoconnectNoStartHigh;
Button systemStatus;

Button synthChanButton4;
Button synthChanButton8;
Button synthChanButton16;

Serial board;

ChannelPopup channelPopup;
PollPopup pollPopup;
RadioConfigBox rcBox;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

public void controlEvent(ControlEvent theEvent) {

  if (theEvent.isFrom("sourceList")) {

    controlPanel.hideAllBoxes();

    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    String str = (String)bob.get("headline");
    // str = str.substring(0, str.length()-5);
    //output("Data Source = " + str);
    int newDataSource = PApplet.parseInt(theEvent.getValue());
    eegDataSource = newDataSource; // reset global eegDataSource to the selected value from the list

    if(newDataSource == DATASOURCE_NORMAL_W_AUX){
      updateToNChan(8);
      chanButton8.color_notPressed = isSelected_color;
      chanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
    } else if(newDataSource == DATASOURCE_GANGLION){
      updateToNChan(4);
      if (isWindows() && isHubInitialized == false) {
        hubInit();
        timeOfSetup = millis();
      }
    } else if(newDataSource == DATASOURCE_PLAYBACKFILE){
      updateToNChan(16);
    } else if(newDataSource == DATASOURCE_SYNTHETIC){
      updateToNChan(16);
      synthChanButton4.color_notPressed = autoFileName.color_notPressed;
      synthChanButton8.color_notPressed = isSelected_color;
      synthChanButton16.color_notPressed = autoFileName.color_notPressed;
    }

    output("The new data source is " + str + " and NCHAN = [" + nchan + "]");
  }

  if (theEvent.isFrom("serialList")) {
    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    openBCI_portName = (String)bob.get("headline");
    output("OpenBCI Port Name = " + openBCI_portName);
  }

  if (theEvent.isFrom("bleList")) {
    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    ganglion_portName = (String)bob.get("headline");
    output("Ganglion Device Name = " + ganglion_portName);
  }

  if (theEvent.isFrom("sdTimes")) {
    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    sdSettingString = (String)bob.get("headline");
    sdSetting = PApplet.parseInt(theEvent.getValue());
    if (sdSetting != 0) {
      output("OpenBCI microSD Setting = " + sdSettingString + " recording time");
    } else {
      output("OpenBCI microSD Setting = " + sdSettingString);
    }
    verbosePrint("SD setting = " + sdSetting);
  }
  if (theEvent.isFrom("networkList")){
    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    String str = (String)bob.get("headline");
    int index = PApplet.parseInt(theEvent.getValue());
    if (index == 0) {
      networkType = 0;
    } else if (index ==1){
      networkType = 1;
    } else if (index == 2){
      networkType = 2;
    } else if (index == 3){
      networkType = 3;
    }
  }

  if (theEvent.isFrom("channelList")){
    int setChannelInt = PApplet.parseInt(theEvent.getValue()) + 1;
    //Map bob = ((MenuList)theEvent.getController()).getItem(int(theEvent.getValue()));
    cp5Popup.get(MenuList.class, "channelList").setVisible(false);
    channelPopup.setClicked(false);
    if(setChannel.wasPressed){
      set_channel(rcBox,setChannelInt);
      setChannel.wasPressed = false;
    }
    else if(ovrChannel.wasPressed){
      set_channel_over(rcBox,setChannelInt);
      ovrChannel.wasPressed = false;
    }
    println("still goin off");

  }

  // if (theEvent.isFrom("pollList")){
  //   int setChannelInt = int(theEvent.getValue());
  //   //Map bob = ((MenuList)theEvent.getController()).getItem(int(theEvent.getValue()));
  //   cp5Popup.get(MenuList.class, "pollList").setVisible(false);
  //   channelPopup.setClicked(false);
  //   set_poll(rcBox,setChannelInt);
  //   setPoll.wasPressed = false;
  // }
}

//------------------------------------------------------------------------
//                            Classes
//------------------------------------------------------------------------

class ControlPanel {

  public int x, y, w, h;
  public boolean isOpen;

  boolean showSourceBox, showSerialBox, showFileBox, showChannelBox, showInitBox;
  PlotFontInfo fontInfo;

  //various control panel elements that are unique to specific datasources
  DataSourceBox dataSourceBox;
  SerialBox serialBox;
  DataLogBox dataLogBox;
  ChannelCountBox channelCountBox;
  InitBox initBox;
  SyntheticChannelCountBox synthChannelCountBox;

  NetworkingBox networkingBoxLive;
  UDPOptionsBox udpOptionsBox;
  OSCOptionsBox oscOptionsBox;
  LSLOptionsBox lslOptionsBox;

  PlaybackFileBox playbackFileBox;
  SDConverterBox sdConverterBox;
  NetworkingBox networkingBoxPlayback;

  BLEBox bleBox;
  DataLogBoxGanglion dataLogBoxGanglion;

  SDBox sdBox;

  boolean drawStopInstructions;

  int globalPadding; //design feature: passed through to all box classes as the global spacing .. in pixels .. for all elements/subelements
  int globalBorder;

  boolean convertingSD = false;

  ControlPanel(DAC_GUI mainClass) {

    x = 3;
    y = 3 + topNav.controlPanelCollapser.but_dy;
    w = topNav.controlPanelCollapser.but_dx;
    h = height - PApplet.parseInt(helpWidget.h);

    if(hasIntroAnimation){
      isOpen = false;
    } else {
      isOpen = true;
    }

    fontInfo = new PlotFontInfo();

    // f1 = createFont("Raleway-SemiBold.otf", 16);
    // f2 = createFont("Raleway-Regular.otf", 15);
    // f3 = createFont("Raleway-SemiBold.otf", 15);

    globalPadding = 10;  //controls the padding of all elements on the control panel
    globalBorder = 0;   //controls the border of all elements in the control panel ... using processing's stroke() instead

    cp5 = new ControlP5(mainClass);
    cp5Popup = new ControlP5(mainClass);
    cp5.setAutoDraw(false);
    // cp5.set
    cp5Popup.setAutoDraw(false);

    //boxes active when eegDataSource = Normal (OpenBCI)
    dataSourceBox = new DataSourceBox(x, y, w, h, globalPadding);
    serialBox = new SerialBox(x + w, dataSourceBox.y, w, h, globalPadding);
    dataLogBox = new DataLogBox(x + w, (serialBox.y + serialBox.h), w, h, globalPadding);
    channelCountBox = new ChannelCountBox(x + w, (dataLogBox.y + dataLogBox.h), w, h, globalPadding);
    synthChannelCountBox = new SyntheticChannelCountBox(x + w, dataSourceBox.y, w, h, globalPadding);
    sdBox = new SDBox(x + w, (channelCountBox.y + channelCountBox.h), w, h, globalPadding);
    networkingBoxLive = new NetworkingBox(x + w, (sdBox.y + sdBox.h), w, 135, globalPadding);
    udpOptionsBox = new UDPOptionsBox(networkingBoxLive.x + networkingBoxLive.w, (sdBox.y + sdBox.h), w-30, networkingBoxLive.h, globalPadding);
    oscOptionsBox = new OSCOptionsBox(networkingBoxLive.x + networkingBoxLive.w, (sdBox.y + sdBox.h), w-30, networkingBoxLive.h, globalPadding);
    lslOptionsBox = new LSLOptionsBox(networkingBoxLive.x + networkingBoxLive.w, (sdBox.y + sdBox.h), w-30, networkingBoxLive.h, globalPadding);


    //boxes active when eegDataSource = Playback
    playbackFileBox = new PlaybackFileBox(x + w, dataSourceBox.y, w, h, globalPadding);
    sdConverterBox = new SDConverterBox(x + w, (playbackFileBox.y + playbackFileBox.h), w, h, globalPadding);
    //networkingBoxPlayback = new NetworkingBox(x + w, (sdConverterBox.y + sdConverterBox.h), w, h, globalPadding);

    rcBox = new RadioConfigBox(x+w, y, w, h, globalPadding);
    channelPopup = new ChannelPopup(x+w, y, w, h, globalPadding);
    pollPopup = new PollPopup(x+w,y,w,h,globalPadding);

    initBox = new InitBox(x, (dataSourceBox.y + dataSourceBox.h), w, h, globalPadding);

    // Ganglion
    bleBox = new BLEBox(x + w, dataSourceBox.y, w, h, globalPadding);
    dataLogBoxGanglion = new DataLogBoxGanglion(x + w, (bleBox.y + bleBox.h), w, h, globalPadding);
  }

  public void resetListItems(){
    serialList.activeItem = -1;
    bleList.activeItem = -1;
  }

  public void open(){
    isOpen = true;
    topNav.controlPanelCollapser.setIsActive(true);
  }

  public void close(){
    isOpen = false;
    topNav.controlPanelCollapser.setIsActive(false);
  }

  public void update() {
    //toggle view of cp5 / serial list selection table
    if (isOpen) { // if control panel is open
      if (!cp5.isVisible()) {  //and cp5 is not visible
        cp5.show(); // shot it
        cp5Popup.show();
      }
    } else { //the opposite of above
      if (cp5.isVisible()) {
        cp5.hide();
        cp5Popup.hide();
      }
    }

    //auto-update serial list
    if(Serial.list().length != serialPorts.length && systemMode != SYSTEMMODE_POSTINIT){
      println("Refreshing port list...");
      refreshPortList();
    }

    //update all boxes if they need to be
    dataSourceBox.update();
    serialBox.update();
    bleBox.update();
    dataLogBox.update();
    channelCountBox.update();
    synthChannelCountBox.update();
    sdBox.update();
    rcBox.update();
    initBox.update();
    networkingBoxLive.update();
    //networkingBoxPlayback.update();

    channelPopup.update();
    serialList.updateMenu();
    bleList.updateMenu();
    dataLogBoxGanglion.update();

    //SD File Conversion
    while (convertingSD == true) {
      convertSDFile();
    }

    if (isHubInitialized && isGanglionObjectInitialized) {
      if (!calledForBLEList) {
        calledForBLEList = true;
        if (ganglion.isHubRunning()) {
          ganglion.searchDeviceStart();
        }
      }
    }
  }

  public void draw() {

    pushStyle();

    noStroke();

    // //dark overlay of rest of interface to indicate it's not clickable
    // fill(0, 0, 0, 185);
    // rect(0, 0, width, height);

    // pushStyle();
    // noStroke();
    // // fill(255);
    // fill(31,69,110);
    // rect(0, 0, width, navBarHeight);
    // popStyle();
    // // image(logo_blue, width/2 - (128/2) - 2, 6, 128, 22);
    // image(logo_white, width/2 - (128/2) - 2, 6, 128, 22);

    // if(colorScheme == COLOR_SCHEME_DEFAULT){
    //   noStroke();
    //   fill(229);
    //   rect(0, 0, width, topNav_h);
    //   stroke(bgColor);
    //   fill(255);
    //   rect(-1, 0, width+2, navBarHeight);
    //   image(logo_blue, width/2 - (128/2) - 2, 6, 128, 22);
    // } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
    //   noStroke();
    //   fill(100);
    //   rect(0, 0, width, topNav_h);
    //   stroke(bgColor);
    //   fill(31,69,110);
    //   rect(-1, 0, width+2, navBarHeight);
    //   image(logo_white, width/2 - (128/2) - 2, 6, 128, 22);
    // }

    initBox.draw();

    if (systemMode == 10) {
      drawStopInstructions = true;
    }

    if (systemMode != 10) { // only draw control panel boxes if system running is false
      dataSourceBox.draw();
      drawStopInstructions = false;
      cp5.setVisible(true);//make sure controlP5 elements are visible
      cp5Popup.setVisible(true);

      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {	//when data source is from OpenBCI
        // hideAllBoxes();
        serialBox.draw();
        // dataLogBox.y = serialBox.y + serialBox.h;
        dataLogBox.draw();
        channelCountBox.draw();
        sdBox.draw();
        networkingBoxLive.draw();
        cp5.get(Textfield.class, "fileName").setVisible(true); //make sure the data file field is visible
        cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is visible

        if(rcBox.isShowing){
          rcBox.draw();
          if(channelPopup.wasClicked()){
            channelPopup.draw();
            cp5Popup.get(MenuList.class, "channelList").setVisible(true);
            cp5Popup.get(MenuList.class, "pollList").setVisible(false);
            cp5.get(MenuList.class, "serialList").setVisible(true); //make sure the serialList menulist is visible
            cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
          }
          else if(pollPopup.wasClicked()){
            pollPopup.draw();
            cp5Popup.get(MenuList.class, "pollList").setVisible(true);
            cp5Popup.get(MenuList.class, "channelList").setVisible(false);
            cp5.get(Textfield.class, "fileName").setVisible(true); //make sure the data file field is visible
            // cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
            cp5.get(MenuList.class, "serialList").setVisible(true); //make sure the serialList menulist is visible
            cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
          }

        }
        cp5.get(Textfield.class, "fileName").setVisible(true); //make sure the data file field is visible
        // cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
        cp5.get(MenuList.class, "serialList").setVisible(true); //make sure the serialList menulist is visible
        cp5.get(MenuList.class, "bleList").setVisible(false); //make sure the serialList menulist is visible
        cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
        cp5.get(MenuList.class, "networkList").setVisible(true); //make sure the SD time record options menulist is visible
        if (networkType == -1){
          cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible
        } else if (networkType == 0){
          cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible

        } else if (networkType == 1){
          cp5.get(Textfield.class, "udp_ip").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "udp_port").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible
          udpOptionsBox.draw();
        } else if (networkType == 2){
          cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_ip").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_port").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_address").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible

          oscOptionsBox.draw();
        } else if (networkType == 3){
          cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_data").setVisible(true); //make sure the SD time record options menulist is visible
          cp5.get(Textfield.class, "lsl_aux").setVisible(true); //make sure the SD time record options menulist is visible
          lslOptionsBox.draw();
        }

      } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) { //when data source is from playback file
        // hideAllBoxes(); //clear lists, so they don't appear
        playbackFileBox.draw();
        sdConverterBox.draw();
        //networkingBoxPlayback.draw();

        //set other CP5 controllers invisible
        // cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
        // cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is visible
        cp5.get(MenuList.class, "serialList").setVisible(false);
        cp5.get(MenuList.class, "sdTimes").setVisible(false);
        cp5.get(MenuList.class, "networkList").setVisible(false);
        cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
        cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible

        cp5Popup.get(MenuList.class, "channelList").setVisible(false);
        cp5Popup.get(MenuList.class, "pollList").setVisible(false);

      } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  //synthetic
        //set other CP5 controllers invisible
        // hideAllBoxes();
        synthChannelCountBox.draw();
      } else if (eegDataSource == DATASOURCE_GANGLION) {
        // hideAllBoxes();
        bleBox.draw();
        // dataLogBox.y = bleBox.y + bleBox.h;
        dataLogBoxGanglion.draw();
        cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
        cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
        cp5.get(MenuList.class, "bleList").setVisible(true); //make sure the bleList menulist is visible

      } else {
        //set other CP5 controllers invisible
        hideAllBoxes();
      }
    } else {
      cp5.setVisible(false); // if isRunning is true, hide all controlP5 elements
      cp5Popup.setVisible(false);
      // cp5Serial.setVisible(false);    //%%%
    }

    //draw the box that tells you to stop the system in order to edit control settings
    if (drawStopInstructions) {
      pushStyle();
      fill(boxColor);
      strokeWeight(1);
      stroke(boxStrokeColor);
      rect(x, y, w, dataSourceBox.h); //draw background of box
      String stopInstructions = "Press the \"STOP SYSTEM\" button to change your data source or edit system settings.";
      textAlign(CENTER, TOP);
      textFont(p4, 14);
      fill(bgColor);
      text(stopInstructions, x + globalPadding*2, y + globalPadding*3, w - globalPadding*4, dataSourceBox.h - globalPadding*4);
      popStyle();
    }

    //draw the ControlP5 stuff
    textFont(p4, 14);
    cp5Popup.draw();
    cp5.draw();

    popStyle();

  }

  public void refreshPortList(){
    serialPorts = new String[Serial.list().length];
    serialPorts = Serial.list();
    serialList.items.clear();
    for (int i = 0; i < serialPorts.length; i++) {
      String tempPort = serialPorts[(serialPorts.length-1) - i]; //list backwards... because usually our port is at the bottom
      serialList.addItem(makeItem(tempPort));
    }
    serialList.updateMenu();
  }

  public void hideAllBoxes() {
    //set other CP5 controllers invisible
    cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
    cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is visible
    cp5.get(MenuList.class, "serialList").setVisible(false);
    cp5.get(MenuList.class, "bleList").setVisible(false);
    cp5.get(MenuList.class, "sdTimes").setVisible(false);
    cp5.get(MenuList.class, "networkList").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "udp_ip").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "udp_port").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "osc_ip").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "osc_port").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "osc_address").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "lsl_data").setVisible(false); //make sure the SD time record options menulist is visible
    cp5.get(Textfield.class, "lsl_aux").setVisible(false); //make sure the SD time record options menulist is visible
    cp5Popup.get(MenuList.class, "channelList").setVisible(false);
    cp5Popup.get(MenuList.class, "pollList").setVisible(false);
  }

  //mouse pressed in control panel
  public void CPmousePressed() {
    verbosePrint("CPmousePressed");

    if (initSystemButton.isMouseHere()) {
      initSystemButton.setIsActive(true);
      initSystemButton.wasPressed = true;
    }

    //only able to click buttons of control panel when system is not running
    if (systemMode != 10) {

      //active buttons during DATASOURCE_NORMAL_W_AUX
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
        if(autoconnect.isMouseHere()){
          autoconnect.setIsActive(true);
          autoconnect.wasPressed = true;
        }

        if (popOut.isMouseHere()){
          popOut.setIsActive(true);
          popOut.wasPressed = true;
        }

        if (refreshPort.isMouseHere()) {
          refreshPort.setIsActive(true);
          refreshPort.wasPressed = true;
        }

        if (autoFileName.isMouseHere()) {
          autoFileName.setIsActive(true);
          autoFileName.wasPressed = true;
        }

        if (outputODF.isMouseHere()) {
          outputODF.setIsActive(true);
          outputODF.wasPressed = true;
          outputODF.color_notPressed = isSelected_color;
          outputBDF.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (outputBDF.isMouseHere()) {
          outputBDF.setIsActive(true);
          outputBDF.wasPressed = true;
          outputBDF.color_notPressed = isSelected_color;
          outputODF.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (chanButton8.isMouseHere()) {
          chanButton8.setIsActive(true);
          chanButton8.wasPressed = true;
          chanButton8.color_notPressed = isSelected_color;
          chanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (chanButton16.isMouseHere()) {
          chanButton16.setIsActive(true);
          chanButton16.wasPressed = true;
          chanButton8.color_notPressed = autoFileName.color_notPressed; //default color of button
          chanButton16.color_notPressed = isSelected_color;
        }

        if (getChannel.isMouseHere()){
          getChannel.setIsActive(true);
          getChannel.wasPressed = true;
        }

        if (setChannel.isMouseHere()){
          setChannel.setIsActive(true);
          setChannel.wasPressed = true;
        }

        if (ovrChannel.isMouseHere()){
          ovrChannel.setIsActive(true);
          ovrChannel.wasPressed = true;
        }

        // if (getPoll.isMouseHere()){
        //   getPoll.setIsActive(true);
        //   getPoll.wasPressed = true;
        // }

        // if (setPoll.isMouseHere()){
        //   setPoll.setIsActive(true);
        //   setPoll.wasPressed = true;
        // }

        // if (defaultBAUD.isMouseHere()){
        //   defaultBAUD.setIsActive(true);
        //   defaultBAUD.wasPressed = true;
        // }

        // if (highBAUD.isMouseHere()){
        //   highBAUD.setIsActive(true);
        //   highBAUD.wasPressed = true;
        // }

        if (autoscan.isMouseHere()){
          autoscan.setIsActive(true);
          autoscan.wasPressed = true;
        }

        // if (autoconnectNoStartDefault.isMouseHere()){
        //   autoconnectNoStartDefault.setIsActive(true);
        //   autoconnectNoStartDefault.wasPressed = true;
        // }

        // if (autoconnectNoStartHigh.isMouseHere()){
        //   autoconnectNoStartHigh.setIsActive(true);
        //   autoconnectNoStartHigh.wasPressed = true;
        // }


        if (systemStatus.isMouseHere()){
          systemStatus.setIsActive(true);
          systemStatus.wasPressed = true;
        }

      }

      if (eegDataSource == DATASOURCE_GANGLION) {
        // This is where we check for button presses if we are searching for BLE devices

        if (autoFileNameGanglion.isMouseHere()) {
          autoFileNameGanglion.setIsActive(true);
          autoFileNameGanglion.wasPressed = true;
        }

        if (outputODFGanglion.isMouseHere()) {
          outputODFGanglion.setIsActive(true);
          outputODFGanglion.wasPressed = true;
          outputODFGanglion.color_notPressed = isSelected_color;
          outputBDFGanglion.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (outputBDFGanglion.isMouseHere()) {
          outputBDFGanglion.setIsActive(true);
          outputBDFGanglion.wasPressed = true;
          outputBDFGanglion.color_notPressed = isSelected_color;
          outputODFGanglion.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (refreshBLE.isMouseHere()) {
          refreshBLE.setIsActive(true);
          refreshBLE.wasPressed = true;
        }

      }

      //active buttons during DATASOURCE_PLAYBACKFILE
      if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
        if (selectPlaybackFile.isMouseHere()) {
          selectPlaybackFile.setIsActive(true);
          selectPlaybackFile.wasPressed = true;
        }

        if (selectSDFile.isMouseHere()) {
          selectSDFile.setIsActive(true);
          selectSDFile.wasPressed = true;
        }
      }

      //active buttons during DATASOURCE_PLAYBACKFILE
      if (eegDataSource == DATASOURCE_SYNTHETIC) {
        if (synthChanButton4.isMouseHere()) {
          synthChanButton4.setIsActive(true);
          synthChanButton4.wasPressed = true;
          synthChanButton4.color_notPressed = isSelected_color;
          synthChanButton8.color_notPressed = autoFileName.color_notPressed; //default color of button
          synthChanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (synthChanButton8.isMouseHere()) {
          synthChanButton8.setIsActive(true);
          synthChanButton8.wasPressed = true;
          synthChanButton8.color_notPressed = isSelected_color;
          synthChanButton4.color_notPressed = autoFileName.color_notPressed; //default color of button
          synthChanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (synthChanButton16.isMouseHere()) {
          synthChanButton16.setIsActive(true);
          synthChanButton16.wasPressed = true;
          synthChanButton16.color_notPressed = isSelected_color;
          synthChanButton4.color_notPressed = autoFileName.color_notPressed; //default color of button
          synthChanButton8.color_notPressed = autoFileName.color_notPressed; //default color of button
        }
      }

    }
    // output("Text File Name: " + cp5.get(Textfield.class,"fileName").getText());
  }

  //mouse released in control panel
  public void CPmouseReleased() {
    //verbosePrint("CPMouseReleased: CPmouseReleased start...");
    if(popOut.isMouseHere() && popOut.wasPressed){
      popOut.wasPressed = false;
      popOut.setIsActive(false);
      if(rcBox.isShowing){
        rcBox.isShowing = false;
        cp5Popup.hide(); // make sure to hide the controlP5 object
        cp5Popup.get(MenuList.class, "channelList").setVisible(false);
        cp5Popup.get(MenuList.class, "pollList").setVisible(false);
        // cp5Popup.hide(); // make sure to hide the controlP5 object
        popOut.setString(">");
      }
      else{
        rcBox.isShowing = true;
        popOut.setString("<");
      }
    }

    if(getChannel.isMouseHere() && getChannel.wasPressed){
      // if(board != null) // Radios_Config will handle creating the serial port JAM 1/2017
      get_channel( rcBox);
      getChannel.wasPressed=false;
      getChannel.setIsActive(false);
    }

    if (setChannel.isMouseHere() && setChannel.wasPressed){
      channelPopup.setClicked(true);
      pollPopup.setClicked(false);
      setChannel.setIsActive(false);
    }

    if (ovrChannel.isMouseHere() && ovrChannel.wasPressed){
      channelPopup.setClicked(true);
      pollPopup.setClicked(false);
      ovrChannel.setIsActive(false);
    }


    // if (getPoll.isMouseHere() && getPoll.wasPressed){
    //   get_poll(rcBox);
    //   getPoll.setIsActive(false);
    //   getPoll.wasPressed = false;
    // }

    // if (setPoll.isMouseHere() && setPoll.wasPressed){
    //   pollPopup.setClicked(true);
    //   channelPopup.setClicked(false);
    //   setPoll.setIsActive(false);
    // }

    // if (defaultBAUD.isMouseHere() && defaultBAUD.wasPressed){
    //   set_baud_default(rcBox,openBCI_portName);
    //   defaultBAUD.setIsActive(false);
    //   defaultBAUD.wasPressed=false;
    // }

    // if (highBAUD.isMouseHere() && highBAUD.wasPressed){
    //   set_baud_high(rcBox,openBCI_portName);
    //   highBAUD.setIsActive(false);
    //   highBAUD.wasPressed=false;
    // }

    // if(autoconnectNoStartDefault.isMouseHere() && autoconnectNoStartDefault.wasPressed){
    //
    //   if(board == null){
    //     try{
    //       board = autoconnect_return_default();
    //       rcBox.print_onscreen("Successfully connected to board");
    //     }
    //     catch (Exception e){
    //       rcBox.print_onscreen("Error connecting to board...");
    //     }
    //
    //
    //   }
    //  else rcBox.print_onscreen("Board already connected!");
    //   autoconnectNoStartDefault.setIsActive(false);
    //   autoconnectNoStartDefault.wasPressed = false;
    // }

    // if(autoconnectNoStartHigh.isMouseHere() && autoconnectNoStartHigh.wasPressed){
    //
    //   if(board == null){
    //
    //     try{
    //
    //       board = autoconnect_return_high();
    //       rcBox.print_onscreen("Successfully connected to board");
    //     }
    //     catch (Exception e2){
    //       rcBox.print_onscreen("Error connecting to board...");
    //     }
    //
    //   }
    //  else rcBox.print_onscreen("Board already connected!");
    //   autoconnectNoStartHigh.setIsActive(false);
    //   autoconnectNoStartHigh.wasPressed = false;
    // }

    if(autoscan.isMouseHere() && autoscan.wasPressed){
      autoscan.wasPressed = false;
      autoscan.setIsActive(false);
      scan_channels(rcBox);

    }

    if(autoconnect.isMouseHere() && autoconnect.wasPressed && eegDataSource != DATASOURCE_PLAYBACKFILE){
      autoconnect();
      initButtonPressed();
      autoconnect.wasPressed = false;
      autoconnect.setIsActive(false);
    }

    if(systemStatus.isMouseHere() && systemStatus.wasPressed){
      system_status(rcBox);
      systemStatus.setIsActive(false);
      systemStatus.wasPressed = false;
    }


    if (initSystemButton.isMouseHere() && initSystemButton.wasPressed) {
      if(board != null) board.stop();
      //if system is not active ... initate system and flip button state
      initButtonPressed();
      //cursor(ARROW); //this this back to ARROW
    }

    //open or close serial port if serial port button is pressed (left button in serial widget)
    if (refreshPort.isMouseHere() && refreshPort.wasPressed) {
      output("Serial/COM List Refreshed");
      refreshPortList();
    }

    //open or close serial port if serial port button is pressed (left button in serial widget)
    if (refreshBLE.isMouseHere() && refreshBLE.wasPressed) {
      if (isGanglionObjectInitialized) {
        output("BLE Devices Refreshing");
        bleList.items.clear();
        ganglion.searchDeviceStart();
      } else {
        output("Please wait till BLE is fully initalized");
      }
    }

    //open or close serial port if serial port button is pressed (left button in serial widget)
    if (autoFileName.isMouseHere() && autoFileName.wasPressed) {
      output("Autogenerated \"File Name\" based on current date/time");
      cp5.get(Textfield.class, "fileName").setText(getDateString());
    }

    if (outputODF.isMouseHere() && outputODF.wasPressed) {
      output("Output has been set to OpenBCI Data Format");
      outputDataSource = OUTPUT_SOURCE_ODF;
    }

    if (outputBDF.isMouseHere() && outputBDF.wasPressed) {
      output("Output has been set to BDF+ (biosemi data format based off EDF)");
      outputDataSource = OUTPUT_SOURCE_BDF;
    }

    if (autoFileNameGanglion.isMouseHere() && autoFileNameGanglion.wasPressed) {
      output("Autogenerated \"File Name\" based on current date/time");
      cp5.get(Textfield.class, "fileNameGanglion").setText(getDateString());
    }

    if (outputODFGanglion.isMouseHere() && outputODFGanglion.wasPressed) {
      output("Output has been set to OpenBCI Data Format");
      outputDataSource = OUTPUT_SOURCE_ODF;
    }

    if (outputBDFGanglion.isMouseHere() && outputBDFGanglion.wasPressed) {
      output("Output has been set to BDF+ (biosemi data format based off EDF)");
      outputDataSource = OUTPUT_SOURCE_BDF;
    }

    if (chanButton8.isMouseHere() && chanButton8.wasPressed) {
      updateToNChan(8);
    }

    if (chanButton16.isMouseHere() && chanButton16.wasPressed ) {
      updateToNChan(16);
    }

    if (synthChanButton4.isMouseHere() && synthChanButton4.wasPressed) {
      updateToNChan(4);
    }

    if (synthChanButton8.isMouseHere() && synthChanButton8.wasPressed) {
      updateToNChan(8);
    }

    if (synthChanButton16.isMouseHere() && synthChanButton16.wasPressed) {
      updateToNChan(16);
    }

    if (selectPlaybackFile.isMouseHere() && selectPlaybackFile.wasPressed) {
      output("select a file for playback");
      selectInput("Select a pre-recorded file for playback:", "playbackSelected");
    }

    if (selectSDFile.isMouseHere() && selectSDFile.wasPressed) {
      output("select an SD file to convert to a playback file");
      createPlaybackFileFromSD();
      selectInput("Select an SD file to convert for playback:", "sdFileSelected");
    }

    //reset all buttons to false
    refreshPort.setIsActive(false);
    refreshPort.wasPressed = false;
    refreshBLE.setIsActive(false);
    refreshBLE.wasPressed = false;
    initSystemButton.setIsActive(false);
    initSystemButton.wasPressed = false;
    autoFileName.setIsActive(false);
    autoFileName.wasPressed = false;
    outputBDF.setIsActive(false);
    outputBDF.wasPressed = false;
    outputODF.setIsActive(false);
    outputODF.wasPressed = false;
    autoFileNameGanglion.setIsActive(false);
    autoFileNameGanglion.wasPressed = false;
    outputBDFGanglion.setIsActive(false);
    outputBDFGanglion.wasPressed = false;
    outputODFGanglion.setIsActive(false);
    outputODFGanglion.wasPressed = false;
    chanButton8.setIsActive(false);
    chanButton8.wasPressed = false;
    synthChanButton4.setIsActive(false);
    synthChanButton4.wasPressed = false;
    synthChanButton8.setIsActive(false);
    synthChanButton8.wasPressed = false;
    synthChanButton16.setIsActive(false);
    synthChanButton16.wasPressed = false;
    chanButton16.setIsActive(false);
    chanButton16.wasPressed  = false;
    selectPlaybackFile.setIsActive(false);
    selectPlaybackFile.wasPressed = false;
    selectSDFile.setIsActive(false);
    selectSDFile.wasPressed = false;
  }
};

public void initButtonPressed(){
  if (initSystemButton.but_txt == "START SYSTEM") {

      if (eegDataSource == DATASOURCE_NORMAL_W_AUX && openBCI_portName == "N/A") { //if data source == normal && if no serial port selected OR no SD setting selected
        output("No Serial/COM port selected. Please select your Serial/COM port and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_PLAYBACKFILE && playbackData_fname == "N/A") { //if data source == playback && playback file == 'N/A'
        output("No playback file selected. Please select a playback file and retry system initiation.");        // tell user that they need to select a file before the system can be started
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_GANGLION && ganglion_portName == "N/A") {
        output("No BLE device selected. Please select your Ganglion device and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      // } else if (eegDataSource == DATASOURCE_SYNTHETIC){
      //   nchan = 16;
      //   output("Starting system with 16 channels of synthetically generated data...");
      //   initSystemButton.wasPressed = false;
      //   initSystemButton.setIsActive(false);
      //   return;
      } else if (eegDataSource == -1) {//if no data source selected
        output("No DATA SOURCE selected. Please select a DATA SOURCE and retry system initiation.");//tell user they must select a data source before initiating system
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else { //otherwise, initiate system!
        //verbosePrint("ControlPanel: CPmouseReleased: init");
        initSystemButton.setString("STOP SYSTEM");
        //global steps to START SYSTEM
        // prepare the serial port
        if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
          verbosePrint("ControlPanel \u2014 port is open: " + openBCI.isSerialPortOpen());
          if (openBCI.isSerialPortOpen() == true) {
            openBCI.closeSerialPort();
          }
        } else if(eegDataSource == DATASOURCE_GANGLION){
          verbosePrint("ControlPanel \u2014 port is open: " + ganglion.isPortOpen());
          if (ganglion.isPortOpen()) {
            ganglion.disconnectBLE();
          } else {
            //do nothing
          }
        }

        //Network Protocol Initiation -- based on Gabe's Code
        if (networkType == 1){
          ip = cp5.get(Textfield.class, "udp_ip").getText();
          port = PApplet.parseInt(cp5.get(Textfield.class, "udp_port").getText());
          println(port);
          udp = new UDPSend(port, ip);
        } else if (networkType == 2){
          ip = cp5.get(Textfield.class, "osc_ip").getText();
          port = PApplet.parseInt(cp5.get(Textfield.class, "osc_port").getText());
          address = cp5.get(Textfield.class, "osc_address").getText();
          osc = new OSCSend(port, ip, address);
        } else if (networkType == 3){
          data_stream = cp5.get(Textfield.class, "lsl_data").getText();
          aux_stream = cp5.get(Textfield.class, "lsl_aux").getText();
          lsl = new LSLSend(data_stream, aux_stream);
        }

        if(eegDataSource == DATASOURCE_GANGLION){
          fileName = cp5.get(Textfield.class, "fileNameGanglion").getText(); // store the current text field value of "File Name" to be passed along to dataFiles
        } else if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
          fileName = cp5.get(Textfield.class, "fileName").getText(); // store the current text field value of "File Name" to be passed along to dataFiles
        }
        midInit = true;
        initSystem(); //calls the initSystem() funciton of the DAC_GUI.pde file
      }
    }

    //if system is already active ... stop system and flip button state back
    else {
      output("SYSTEM STOPPED");
      initSystemButton.setString("START SYSTEM");
      cp5.get(Textfield.class, "fileName").setText(getDateString()); //creates new data file name so that you don't accidentally overwrite the old one
      cp5.get(Textfield.class, "fileNameGanglion").setText(getDateString()); //creates new data file name so that you don't accidentally overwrite the old one
      if(eegDataSource == DATASOURCE_GANGLION){
        if(ganglion.isCheckingImpedance()){
          ganglion.impedanceStop();
          w_ganglionImpedance.startStopCheck.but_txt = "Start Impedance Check";
        }
      }
      haltSystem();
      if(eegDataSource == DATASOURCE_GANGLION){
        ganglion.searchDeviceStart();
        bleList.items.clear();
      }
    }
}

public void updateToNChan(int _nchan) {
  nchan = _nchan;
  fftBuff = new FFT[nchan];  //reinitialize the FFT buffer
  yLittleBuff_uV = new float[nchan][nPointsPerUpdate];
  output("channel count set to " + str(nchan));
  updateChannelArrays(nchan); //make sure to reinitialize the channel arrays with the right number of channels
}

public void set_channel_popup(){;
}


//==============================================================================//
//					BELOW ARE THE CLASSES FOR THE VARIOUS 						//
//					CONTROL PANEL BOXes (control widgets)						//
//==============================================================================//

class DataSourceBox {
  int x, y, w, h, padding; //size and position
  int numItems = 4;
  int boxHeight = 24;
  int spacing = 43;


  CheckBox sourceCheckBox;

  DataSourceBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = spacing + (numItems * boxHeight);
    padding = _padding;

    sourceList = new MenuList(cp5, "sourceList", w - padding*2, numItems * boxHeight, p4);
    // sourceList.itemHeight = 28;
    // sourceList.padding = 9;
    sourceList.setPosition(x + padding, y + padding*2 + 13);
    sourceList.addItem(makeItem("LIVE (from Cyton)"));
    sourceList.addItem(makeItem("LIVE (from Ganglion)"));
    sourceList.addItem(makeItem("PLAYBACK (from file)"));
    sourceList.addItem(makeItem("SYNTHETIC (algorithmic)"));

    sourceList.scrollerLength = 10;
  }

  public void update() {

  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("DATA SOURCE", x + padding, y + padding);
    popStyle();
    //draw contents of Data Source Box at top of control panel
    //Title
    //checkboxes of system states
  }
};

class SerialBox {
  int x, y, w, h, padding; //size and position
  //connect/disconnect button
  //Refresh list button
  //String port status;

  SerialBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 171 + _padding;
    padding = _padding;

    autoconnect = new Button(x + padding, y + padding*3 + 4, w - padding*2, 24, "AUTOCONNECT AND START SYSTEM", fontInfo.buttonLabel_size);
    refreshPort = new Button (x + padding, y + padding*4 + 13 + 71 + 24, w - padding*2, 24, "REFRESH LIST", fontInfo.buttonLabel_size);
    popOut = new Button(x+padding + (w-padding*4), y + padding, 20,20,">",fontInfo.buttonLabel_size);

    serialList = new MenuList(cp5, "serialList", w - padding*2, 72, p4);
    // println(w-padding*2);
    serialList.setPosition(x + padding, y + padding*3 + 13 + 24);
    serialPorts = Serial.list();
    for (int i = 0; i < serialPorts.length; i++) {
      String tempPort = serialPorts[(serialPorts.length-1) - i]; //list backwards... because usually our port is at the bottom
      serialList.addItem(makeItem(tempPort));
    }
  }

  public void update() {
    // serialList.updateMenu();
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("SERIAL/COM PORT", x + padding, y + padding);
    popStyle();

    // openClosePort.draw();
    refreshPort.draw();
    autoconnect.draw();
    popOut.draw();
  }

  public void refreshSerialList() {
  }
};

class BLEBox {
  int x, y, w, h, padding; //size and position
  //connect/disconnect button
  //Refresh list button
  //String port status;

  BLEBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 171 - 24 + _padding;
    padding = _padding;

    refreshBLE = new Button (x + padding, y + padding * 4 + 13 + 71, w - padding * 2, 24, "REFRESH LIST", fontInfo.buttonLabel_size);
    bleList = new MenuList(cp5, "bleList", w - padding * 2, 84, p4);
    // println(w-padding*2);
    bleList.setPosition(x + padding, y + padding * 3);
    // Call to update the list
    // ganglion.getBLEDevices();
  }

  public void update() {
    // Quick check to see if there are just more or less devices in general

  }

  public void updateListPosition(){
    bleList.setPosition(x + padding, y + padding * 3);
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("BLE DEVICES", x + padding, y + padding);
    popStyle();

    refreshBLE.draw();
  }

  public void refreshBLEList() {
    bleList.items.clear();
    for (int i = 0; i < ganglion.deviceList.length; i++) {
      String tempPort = ganglion.deviceList[i];
      bleList.addItem(makeItem(tempPort));
    }
    bleList.updateMenu();
  }
};

class DataLogBox {
  int x, y, w, h, padding; //size and position
  String fileName;
  //text field for inputing text
  //create/open/closefile button
  String fileStatus;
  boolean isFileOpen; //true if file has been activated and is ready to write to
  //String port status;

  DataLogBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 127; // Added 24 +
    padding = _padding;
    //instantiate button
    //figure out default file name (from Chip's code)
    isFileOpen = false; //set to true on button push
    fileStatus = "NO FILE CREATED";

    //button to autogenerate file name based on time/date
    autoFileName = new Button (x + padding, y + 66, w-(padding*2), 24, "AUTOGENERATE FILE NAME", fontInfo.buttonLabel_size);
    outputODF = new Button (x + padding, y + padding*2 + 18 + 58, (w-padding*3)/2, 24, "OpenBCI", fontInfo.buttonLabel_size);
    if (outputDataSource == OUTPUT_SOURCE_ODF) outputODF.color_notPressed = isSelected_color; //make it appear like this one is already selected
    outputBDF = new Button (x + padding*2 + (w-padding*3)/2, y + padding*2 + 18 + 58, (w-padding*3)/2, 24, "BDF+", fontInfo.buttonLabel_size);
    if (outputDataSource == OUTPUT_SOURCE_BDF) outputBDF.color_notPressed = isSelected_color; //make it appear like this one is already selected


    cp5.addTextfield("fileName")
      .setPosition(x + 90, y + 32)
      .setCaptionLabel("")
      .setSize(157, 26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26, 26, 26))
      .setColorBackground(color(255, 255, 255)) // text field bg color
      .setColorValueLabel(color(0, 0, 0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26, 26, 26))
      .setText(getDateString())
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setAutoClear(true);

    //clear text field on double click
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("DATA LOG FILE", x + padding, y + padding);
    textFont(p4, 14);;
    text("File Name", x + padding, y + padding*2 + 14);
    popStyle();
    cp5.get(Textfield.class, "fileName").setPosition(x + 90, y + 32);
    autoFileName.but_y = y + 66;
    autoFileName.draw();
    outputODF.but_y = y + padding*2 + 18 + 58;
    outputODF.draw();
    outputBDF.but_y = y + padding*2 + 18 + 58;
    outputBDF.draw();
  }
};

class DataLogBoxGanglion {
  int x, y, w, h, padding; //size and position
  String fileName;
  //text field for inputing text
  //create/open/closefile button
  String fileStatus;
  boolean isFileOpen; //true if file has been activated and is ready to write to
  //String port status;

  DataLogBoxGanglion(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 127; // Added 24 +
    padding = _padding;
    //instantiate button
    //figure out default file name (from Chip's code)
    isFileOpen = false; //set to true on button push
    fileStatus = "NO FILE CREATED";

    //button to autogenerate file name based on time/date
    autoFileNameGanglion = new Button (x + padding, y + 66, w-(padding*2), 24, "AUTOGENERATE FILE NAME", fontInfo.buttonLabel_size);
    outputODFGanglion = new Button (x + padding, y + padding*2 + 18 + 58, (w-padding*3)/2, 24, "OpenBCI", fontInfo.buttonLabel_size);
    if (outputDataSource == OUTPUT_SOURCE_ODF) outputODFGanglion.color_notPressed = isSelected_color; //make it appear like this one is already selected
    outputBDFGanglion = new Button (x + padding*2 + (w-padding*3)/2, y + padding*2 + 18 + 58, (w-padding*3)/2, 24, "BDF+", fontInfo.buttonLabel_size);
    if (outputDataSource == OUTPUT_SOURCE_BDF) outputODFGanglion.color_notPressed = isSelected_color; //make it appear like this one is already selected


    cp5.addTextfield("fileNameGanglion")
      .setPosition(x + 90, y + 32)
      .setCaptionLabel("")
      .setSize(157, 26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26, 26, 26))
      .setColorBackground(color(255, 255, 255)) // text field bg color
      .setColorValueLabel(color(0, 0, 0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26, 26, 26))
      .setText(getDateString())
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setAutoClear(true);

    //clear text field on double click
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("DATA LOG FILE", x + padding, y + padding);
    textFont(p4, 14);;
    text("File Name", x + padding, y + padding*2 + 14);
    popStyle();
    cp5.get(Textfield.class, "fileNameGanglion").setPosition(x + 90, y + 32);
    autoFileNameGanglion.but_y = y + 66;
    autoFileNameGanglion.draw();
    outputODFGanglion.but_y = y + padding*2 + 18 + 58;
    outputODFGanglion.draw();
    outputBDFGanglion.but_y = y + padding*2 + 18 + 58;
    outputBDFGanglion.draw();
  }
};

class ChannelCountBox {
  int x, y, w, h, padding; //size and position

  boolean isSystemInitialized;
  // button for init/halt system

  ChannelCountBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    chanButton8 = new Button (x + padding, y + padding*2 + 18, (w-padding*3)/2, 24, "8 CHANNELS", fontInfo.buttonLabel_size);
    if (nchan == 8) chanButton8.color_notPressed = isSelected_color; //make it appear like this one is already selected
    chanButton16 = new Button (x + padding*2 + (w-padding*3)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "16 CHANNELS", fontInfo.buttonLabel_size);
    if (nchan == 16) chanButton16.color_notPressed = isSelected_color; //make it appear like this one is already selected
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("CHANNEL COUNT ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  (" + str(nchan) + ")", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    chanButton8.draw();
    chanButton16.draw();
  }
};

class SyntheticChannelCountBox {
  int x, y, w, h, padding; //size and position

  boolean isSystemInitialized;
  // button for init/halt system

  SyntheticChannelCountBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    synthChanButton4 = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "4 chan", fontInfo.buttonLabel_size);
    if (nchan == 4) synthChanButton4.color_notPressed = isSelected_color; //make it appear like this one is already selected
    synthChanButton8 = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "8 chan", fontInfo.buttonLabel_size);
    if (nchan == 8) synthChanButton8.color_notPressed = isSelected_color; //make it appear like this one is already selected
    synthChanButton16 = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "16 chan", fontInfo.buttonLabel_size);
    if (nchan == 16) synthChanButton16.color_notPressed = isSelected_color; //make it appear like this one is already selected
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("CHANNEL COUNT", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  (" + str(nchan) + ")", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    synthChanButton4.draw();
    synthChanButton8.draw();
    synthChanButton16.draw();
  }
};

class PlaybackFileBox {
  int x, y, w, h, padding; //size and position

  PlaybackFileBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 67;
    padding = _padding;

    selectPlaybackFile = new Button (x + padding, y + padding*2 + 13, w - padding*2, 24, "SELECT PLAYBACK FILE", fontInfo.buttonLabel_size);
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("PLAYBACK FILE", x + padding, y + padding);
    popStyle();

    selectPlaybackFile.draw();
    // chanButton16.draw();
  }
};

class SDBox {
  int x, y, w, h, padding; //size and position

  SDBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 150;
    padding = _padding;

    sdTimes = new MenuList(cp5, "sdTimes", w - padding*2, 108, p4);
    sdTimes.setPosition(x + padding, y + padding*2 + 13);
    serialPorts = Serial.list();

    //add items for the various SD times
    sdTimes.addItem(makeItem("Do not write to SD..."));
    sdTimes.addItem(makeItem("5 minute maximum"));
    sdTimes.addItem(makeItem("15 minute maximum"));
    sdTimes.addItem(makeItem("30 minute maximum"));
    sdTimes.addItem(makeItem("1 hour maximum"));
    sdTimes.addItem(makeItem("2 hours maximum"));
    sdTimes.addItem(makeItem("4 hour maximum"));
    sdTimes.addItem(makeItem("12 hour maximum"));
    sdTimes.addItem(makeItem("24 hour maximum"));

    sdTimes.activeItem = sdSetting; //added to indicate default choice (sdSetting is in DAC_GUI)
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("WRITE TO SD (Y/N)?", x + padding, y + padding);
    popStyle();

    //the drawing of the sdTimes is handled earlier in ControlPanel.draw()

  }
};

class NetworkingBox{
  int x, y, w, h, padding; //size and position
   MenuList networkList;

  //boolean initButtonPressed; //default false

  //boolean isSystemInitialized;
  NetworkingBox(int _x, int _y, int _w, int _h, int _padding){
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    padding = _padding;
    networkList = new MenuList(cp5, "networkList", w - padding*2, 96, p4);
    networkList.setPosition(x + padding, y+padding+20);
    networkList.addItem(makeItem("None"));
    networkList.addItem(makeItem("UDP"));
    networkList.addItem(makeItem("OSC"));
    networkList.addItem(makeItem("LabStreamingLayer (LSL)"));
    networkList.scrollerLength = 0;
    networkList.activeItem = 0;
  }
  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("NETWORK PROTOCOLS", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    popStyle();
  }
};


class RadioConfigBox {
  int x, y, w, h, padding; //size and position
  String last_message = "";
  Serial board;
  boolean isShowing;

  RadioConfigBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x + _w;
    y = _y;
    w = _w;
    h = 255;
    padding = _padding;
    isShowing = false;

    getChannel = new Button(x + padding, y + padding*2 + 18, (w-padding*3)/2, 24, "GET CHANNEL", fontInfo.buttonLabel_size);
    systemStatus = new Button(x + padding + (w-padding*2)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "STATUS", fontInfo.buttonLabel_size);
    setChannel = new Button(x + padding, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "CHANGE CHANNEL", fontInfo.buttonLabel_size);
    ovrChannel = new Button(x + padding, y + padding*4 + 18 + 24*2, (w-padding*3)/2, 24, "OVERRIDE DONGLE", fontInfo.buttonLabel_size);
    autoscan = new Button(x + padding + (w-padding*2)/2, y + padding*4 + 18 + 24*2, (w-padding*3)/2, 24, "AUTOSCAN", fontInfo.buttonLabel_size);
    // getPoll = new Button(x + padding + (w-padding*2)/2, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "GET POLL", fontInfo.buttonLabel_size);
    // highBAUD = new Button(x + padding, y + padding*5 + 18 + 24*3, (w-padding*3)/2, 24, "HIGH BAUD", fontInfo.buttonLabel_size);
    // setPoll = new Button(x + padding + (w-padding*2)/2, y + padding*5 + 18 + 24*3, (w-padding*3)/2, 24, "", fontInfo.buttonLabel_size);
    // autoconnectNoStartDefault = new Button(x + padding, y + padding*6 + 18 + 24*4, (w-padding*3 )/2 , 24, "CONNECT 115200", fontInfo.buttonLabel_size);
    // deraultBaud = new Button(x + padding + (w-padding*2)/2, y + padding*6 + 18 + 24*4, (w-padding*3 )/2, 24, "", fontInfo.buttonLabel_size);
    // autoconnectNoStartHigh = new Button(x + padding, y + padding*7 + 18 + 24*5, (w-padding*3 )/2, 24, "CONNECT 230400", fontInfo.buttonLabel_size);

    //Set help text
    getChannel.setHelpText("Get the current channel of your Cyton and USB Dongle");
    setChannel.setHelpText("Change the channel of your Cyton and USB Dongle");
    ovrChannel.setHelpText("Change the channel of the USB Dongle only");
    autoscan.setHelpText("Scan through channels and connect to a nearby Cyton");
    systemStatus.setHelpText("Get the connection status of your Cyton system");
    // getPoll.setHelpText("Gets the current POLL value.");
    // setPoll.setHelpText("Sets the current POLL value.");
    // defaultBAUD.setHelpText("Sets the BAUD rate to 115200.");
    // highBAUD.setHelpText("Sets the BAUD rate to 230400.");
    // autoconnectNoStartDefault.setHelpText("Automatically connects to a board with the DEFAULT (115200) BAUD");
    // autoconnectNoStartHigh.setHelpText("Automatically connects to a board with the HIGH (230400) BAUD");

  }
  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("RADIO CONFIGURATION (v2)", x + padding, y + padding);
    popStyle();
    getChannel.draw();
    setChannel.draw();
    ovrChannel.draw();
    systemStatus.draw();
    autoscan.draw();
    // getPoll.draw();
    // setPoll.draw();
    // defaultBAUD.draw();
    // highBAUD.draw();
    // autoconnectNoStartDefault.draw();
    // autoconnectNoStartHigh.draw();

    this.print_onscreen(last_message);

    //the drawing of the sdTimes is handled earlier in ControlPanel.draw()

  }

  public void print_onscreen(String localstring){
    textAlign(LEFT);
    fill(0);
    rect(x + padding, y + (padding*8) + 18 + (24*2), (w-padding*3 + 5), 135 - 24 - padding);
    fill(255);
    text(localstring, x + padding + 10, y + (padding*8) + 18 + (24*2) + 15, (w-padding*3 ), 135 - 24 - padding -15);
    this.last_message = localstring;
  }

  public void print_lastmessage(){

    fill(0);
    rect(x + padding, y + (padding*7) + 18 + (24*5), (w-padding*3 + 5), 135);
    fill(255);
    text(this.last_message, 180, 340, 240, 60);
  }
};



class UDPOptionsBox {
  int x, y, w, h, padding; //size and position

  UDPOptionsBox(int _x, int _y, int _w, int _h, int _padding){
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    padding = _padding;

    cp5.addTextfield("udp_ip")
      .setPosition(x + 60,y + 50)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("localhost")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;

   cp5.addTextfield("udp_port")
      .setPosition(x + 60,y + 82)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("12345")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;
  }
  public void update(){
  }
  public void draw(){
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("Options", x + padding, y + padding);
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    text("UDP OPTIONS", x + padding, y + padding);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("IP", x + padding, y + 50 + padding);
    textFont(p4, 14);;
    text("Port", x + padding, y + 82 + padding);
    popStyle();
  }
};

class OSCOptionsBox{
  int x, y, w, h, padding; //size and position

  OSCOptionsBox(int _x, int _y, int _w, int _h, int _padding){
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    padding = _padding;

    cp5.addTextfield("osc_ip")
      .setPosition(x + 80,y + 35)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("localhost")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;
   cp5.addTextfield("osc_port")
      .setPosition(x + 80,y + 67)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("12345")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;
    cp5.addTextfield("osc_address")
      .setPosition(x + 80,y + 99)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("/openbci")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;
  }
  public void update(){
  }
  public void draw(){
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("Options", x + padding, y + padding);
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    text("OSC OPTIONS", x + padding, y + padding);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("IP", x + padding, y + 35 + padding);
    textFont(p4, 14);;
    text("Port", x + padding, y + 67 + padding);
    text("Address", x + padding, y + 99 + padding);
    popStyle();
  }
};

class LSLOptionsBox {
  int x, y, w, h, padding; //size and position

  LSLOptionsBox(int _x, int _y, int _w, int _h, int _padding){
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    padding = _padding;

    cp5.addTextfield("lsl_data")
      .setPosition(x + 115,y + 50)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("openbci_data")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;

   cp5.addTextfield("lsl_aux")
      .setPosition(x + 115,y + 82)
      .setCaptionLabel("")
      .setSize(100,26)
      .setFont(f2)
      .setFocus(false)
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))  // text color
      .setColorForeground(isSelected_color)  // border color when not selected
      .setColorActive(isSelected_color)  // border color when selected
      .setColorCursor(color(26,26,26))
      .setText("openbci_aux")
      .align(5, 10, 20, 40)
      .onDoublePress(cb)
      .setVisible(false)
      .setAutoClear(true)
      ;
  }
  public void update(){
  }
  public void draw(){
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("Options", x + padding, y + padding);
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    text("LSL OPTIONS", x + padding, y + padding);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("Data Stream", x + padding, y + 50 + padding);
    textFont(p4, 14);;
    text("Aux Stream", x + padding, y + 82 + padding);
    popStyle();
  }
};

class SDConverterBox {
  int x, y, w, h, padding; //size and position

  SDConverterBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 67;
    padding = _padding;

    selectSDFile = new Button (x + padding, y + padding*2 + 13, w - padding*2, 24, "SELECT SD FILE", fontInfo.buttonLabel_size);
  }

  public void update() {
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("CONVERT SD FOR PLAYBACK", x + padding, y + padding);
    popStyle();

    selectSDFile.draw();
  }
};


class ChannelPopup {
  int x, y, w, h, padding; //size and position
  //connect/disconnect button
  //Refresh list button
  //String port status;
  boolean clicked;

  ChannelPopup(int _x, int _y, int _w, int _h, int _padding) {
    x = _x + _w * 2;
    y = _y;
    w = _w;
    h = 171 + _padding;
    padding = _padding;
    clicked = false;

    channelList = new MenuList(cp5Popup, "channelList", w - padding*2, 140, p4);
    channelList.setPosition(x+padding, y+padding*3);

    for (int i = 1; i < 26; i++) {
      channelList.addItem(makeItem(String.valueOf(i)));
    }
  }

  public void update() {
    // serialList.updateMenu();
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("CHANNEL SELECTION", x + padding, y + padding);
    popStyle();

    // openClosePort.draw();
    refreshPort.draw();
    autoconnect.draw();
  }

  public void setClicked(boolean click){this.clicked = click; }

  public boolean wasClicked(){return this.clicked;}

};

class PollPopup {
  int x, y, w, h, padding; //size and position
  //connect/disconnect button
  //Refresh list button
  //String port status;
  boolean clicked;

  PollPopup(int _x, int _y, int _w, int _h, int _padding) {
    x = _x + _w * 2;
    y = _y;
    w = _w;
    h = 171 + _padding;
    padding = _padding;
    clicked = false;


    pollList = new MenuList(cp5Popup, "pollList", w - padding*2, 140, p4);
    pollList.setPosition(x+padding, y+padding*3);

    for (int i = 0; i < 256; i++) {
      pollList.addItem(makeItem(String.valueOf(i)));
    }
  }

  public void update() {
    // serialList.updateMenu();
  }

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("POLL SELECTION", x + padding, y + padding);
    popStyle();

    // openClosePort.draw();
    refreshPort.draw();
    autoconnect.draw();
  }

  public void setClicked(boolean click){this.clicked = click; }

  public boolean wasClicked(){return this.clicked;}

};


class InitBox {
  int x, y, w, h, padding; //size and position

  boolean initButtonPressed; //default false

  boolean isSystemInitialized;
  // button for init/halt system

  InitBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 50;
    padding = _padding;

    //init button
    initSystemButton = new Button (padding, y + padding, w-padding*2, h - padding*2, "START SYSTEM", fontInfo.buttonLabel_size);
    //initSystemButton.color_notPressed = color(boolor);
    //initSystemButton.buttonStrokeColor = color(boxColor);
    initButtonPressed = false;
  }

  public void update() {
  }

  public void draw() {

    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    popStyle();
    initSystemButton.draw();
  }
};



//===================== MENU LIST CLASS =============================//
//================== EXTENSION OF CONTROLP5 =========================//
//============== USED FOR SOURCEBOX & SERIALBOX =====================//
//
// Created: Conor Russomanno Oct. 2014
// Based on ControlP5 Processing Library example, written by Andreas Schlegel
//
/////////////////////////////////////////////////////////////////////

//makeItem function used by MenuList class below
public Map<String, Object> makeItem(String theHeadline) {
  Map m = new HashMap<String, Object>();
  m.put("headline", theHeadline);
  return m;
}

//=======================================================================================================================================
//
//                    MenuList Class
//
//The MenuList class is implemented by the Control Panel. It allows you to set up a list of selectable items within a fixed rectangle size
//Currently used for Serial/COM select, SD settings, and System Mode
//
//=======================================================================================================================================

public class MenuList extends controlP5.Controller {

  float pos, npos;
  int itemHeight = 24;
  int scrollerLength = 40;
  int scrollerWidth = 15;
  List< Map<String, Object>> items = new ArrayList< Map<String, Object>>();
  PGraphics menu;
  boolean updateMenu;
  boolean drawHand;
  int hoverItem = -1;
  int activeItem = -1;
  PFont menuFont = p4;
  int padding = 7;


  MenuList(ControlP5 c, String theName, int theWidth, int theHeight, PFont theFont) {

    super( c, theName, 0, 0, theWidth, theHeight );
    c.register( this );
    menu = createGraphics(getWidth(),getHeight());

    menuFont = p4;
    getValueLabel().setSize(14);
    getCaptionLabel().setSize(14);

    setView(new ControllerView<MenuList>() {

      public void display(PGraphics pg, MenuList t) {
        if (updateMenu) {
          updateMenu();
        }
        if (inside()) {
          // if(!drawHand){
          //   cursor(HAND);
          //   drawHand = true;
          // }
          menu.beginDraw();
          int len = -(itemHeight * items.size()) + getHeight();
          int ty;
          if(len != 0){
            ty = PApplet.parseInt(map(pos, len, 0, getHeight() - scrollerLength - 2, 2 ) );
          } else {
            ty = 0;
          }
          menu.fill(bgColor, 100);
          if(ty > 0){
            menu.rect(getWidth()-scrollerWidth-2, ty, scrollerWidth, scrollerLength );
          }
          menu.endDraw();
        }
        else {
          // if(drawHand){
          //   drawHand = false;
          //   cursor(ARROW);
          // }
        }
        pg.image(menu, 0, 0);
      }
    }
    );
    updateMenu();
  }

  /* only update the image buffer when necessary - to save some resources */
  public void updateMenu() {
    int len = -(itemHeight * items.size()) + getHeight();
    npos = constrain(npos, len, 0);
    pos += (npos - pos) * 0.1f;
    //    pos += (npos - pos) * 0.1;
    menu.beginDraw();
    menu.noStroke();
    menu.background(255, 64);
    // menu.textFont(cp5.getFont().getFont());
    menu.textFont(menuFont);
    menu.pushMatrix();
    menu.translate( 0, pos );
    menu.pushMatrix();

    int i0;
    if((itemHeight * items.size()) != 0){
      i0 = PApplet.max( 0, PApplet.parseInt(map(-pos, 0, itemHeight * items.size(), 0, items.size())));
    } else{
      i0 = 0;
    }
    int range = ceil((PApplet.parseFloat(getHeight())/PApplet.parseFloat(itemHeight))+1);
    int i1 = PApplet.min( items.size(), i0 + range );

    menu.translate(0, i0*itemHeight);

    for (int i=i0; i<i1; i++) {
      Map m = items.get(i);
      menu.fill(255, 100);
      if (i == hoverItem) {
        menu.fill(127, 134, 143);
      }
      if (i == activeItem) {
        menu.stroke(184, 220, 105, 255);
        menu.strokeWeight(1);
        menu.fill(184, 220, 105, 255);
        menu.rect(0, 0, getWidth()-1, itemHeight-1 );
        menu.noStroke();
      } else {
        menu.rect(0, 0, getWidth(), itemHeight-1 );
      }
      menu.fill(bgColor);
      menu.textFont(menuFont);

      //make sure there is something in the Ganglion serial list...
      try {
        menu.text(m.get("headline").toString(), 8, itemHeight - padding); // 5/17
        menu.translate( 0, itemHeight );
      } catch(Exception e){
        println("Nothing in list...");
      }


    }
    menu.popMatrix();
    menu.popMatrix();
    menu.endDraw();
    updateMenu = abs(npos-pos)>0.01f ? true:false;
  }

  /* when detecting a click, check if the click happend to the far right, if yes, scroll to that position,
   * otherwise do whatever this item of the list is supposed to do.
   */
  public void onClick() {
    println("click");
    try{
      if (getPointer().x()>getWidth()-scrollerWidth) {
        if(getHeight() != 0){
          npos= -map(getPointer().y(), 0, getHeight(), 0, items.size()*itemHeight);
        }
        updateMenu = true;
      } else {
        int len = itemHeight * items.size();
        int index = 0;
        if(len != 0){
          index = PApplet.parseInt( map( getPointer().y() - pos, 0, len, 0, items.size() ) ) ;
        }
        setValue(index);
        activeItem = index;
      }
      updateMenu = true;
    } finally{}
    // catch(IOException e){
    //   println("Nothing to click...");
    // }
  }

  public void onMove() {
    if (getPointer().x()>getWidth() || getPointer().x()<0 || getPointer().y()<0  || getPointer().y()>getHeight() ) {
      hoverItem = -1;
    } else {
      int len = itemHeight * items.size();
      int index = 0;
      if(len != 0){
        index = PApplet.parseInt( map( getPointer().y() - pos, 0, len, 0, items.size() ) ) ;
      }
      hoverItem = index;
    }
    updateMenu = true;
  }

  public void onDrag() {
    if (getPointer().x() > (getWidth()-scrollerWidth)) {
      npos= -map(getPointer().y(), 0, getHeight(), 0, items.size()*itemHeight);
      updateMenu = true;
    } else {
      npos += getPointer().dy() * 2;
      updateMenu = true;
    }
  }

  public void onScroll(int n) {
    npos += ( n * 4 );
    updateMenu = true;
  }

  public void addItem(Map<String, Object> m) {
    items.add(m);
    updateMenu = true;
  }

  public void removeItem(Map<String, Object> m) {
    items.remove(m);
    updateMenu = true;
  }

  public Map<String, Object> getItem(int theIndex) {
    return items.get(theIndex);
  }
};

////////////////////////////////////////////////////////////
// Class: OutputFile_rawtxt
// Purpose: handle file creation and writing for the text log file
// Created: Chip Audette  May 2, 2014
//
// DATA FORMAT:
//
////////////////////////////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------






DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

public void openNewLogFile(String _fileName) {
  //close the file if it's open
  switch (outputDataSource) {
    case OUTPUT_SOURCE_ODF:
      openNewLogFileODF(_fileName);
      break;
    case OUTPUT_SOURCE_BDF:
      openNewLogFileBDF(_fileName);
      break;
    case OUTPUT_SOURCE_NONE:
    default:
      // Do nothing...
      break;
  }
}

/**
 * @description Opens (and closes if already open) and ODF file. ODF is the
 *  openbci data format.
 * @param `_fileName` {String} - The meat of the file name
 */
public void openNewLogFileBDF(String _fileName) {
  if (fileoutput_bdf != null) {
    println(appName + ": closing log file");
    closeLogFile();
  }
  //open the new file
  if (eegDataSource == DATASOURCE_GANGLION) {
    fileoutput_bdf = new OutputFile_BDF(ganglion.get_fs_Hz(), nchan, _fileName);
  } else {
    fileoutput_bdf = new OutputFile_BDF(openBCI.get_fs_Hz(), nchan, _fileName);
  }
  output_fname = fileoutput_bdf.fname;
  println("openBCI: openNewLogFile: opened BDF output file: " + output_fname);
  output("openBCI: openNewLogFile: opened BDF output file: " + output_fname);
}

/**
 * @description Opens (and closes if already open) and ODF file. ODF is the
 *  openbci data format.
 * @param `_fileName` {String} - The meat of the file name
 */
public void openNewLogFileODF(String _fileName) {
  if (fileoutput_odf != null) {
    println(appName + ": closing log file");
    closeLogFile();
  }
  //open the new file
  if (eegDataSource == DATASOURCE_GANGLION) {
    fileoutput_odf = new OutputFile_rawtxt(ganglion.get_fs_Hz(), _fileName);
  } else {
    fileoutput_odf = new OutputFile_rawtxt(openBCI.get_fs_Hz(), _fileName);
  }
  output_fname = fileoutput_odf.fname;
  println("openBCI: openNewLogFile: opened ODF output file: " + output_fname);
  output("openBCI: openNewLogFile: opened ODF output file: " + output_fname);
}

/**
 * @description Opens (and closes if already open) and BDF file. BDF is the
 *  biosemi data format.
 * @param `_fileName` {String} - The meat of the file name
 */
public void playbackSelected(File selection) {
  if (selection == null) {
    println("DataLogging: playbackSelected: Window was closed or the user hit cancel.");
  } else {
    println("DataLogging: playbackSelected: User selected " + selection.getAbsolutePath());
    output("You have selected \"" + selection.getAbsolutePath() + "\" for playback.");
    playbackData_fname = selection.getAbsolutePath();
  }
}

public void closeLogFile() {
  switch (outputDataSource) {
    case OUTPUT_SOURCE_ODF:
      closeLogFileODF();
      break;
    case OUTPUT_SOURCE_BDF:
      closeLogFileBDF();
      break;
    case OUTPUT_SOURCE_NONE:
    default:
      // Do nothing...
      break;
  }
}

/**
 * @description Close an open BDF file. This will also update the number of data
 *  records.
 */
public void closeLogFileBDF() {
  if (fileoutput_bdf != null) {
    //TODO: Need to update the rows written in the header
    fileoutput_bdf.closeFile();
  }
}

/**
 * @description Close an open ODF file.
 */
public void closeLogFileODF() {
  if (fileoutput_odf != null) {
    fileoutput_odf.closeFile();
  }
}

public void fileSelected(File selection) {  //called by the Open File dialog box after a file has been selected
  if (selection == null) {
    println("fileSelected: no selection so far...");
  } else {
    //inputFile = selection;
    playbackData_fname = selection.getAbsolutePath();
  }
}

public String getDateString() {
  String fname = year() + "-";
  if (month() < 10) fname=fname+"0";
  fname = fname + month() + "-";
  if (day() < 10) fname = fname + "0";
  fname = fname + day();

  fname = fname + "_";
  if (hour() < 10) fname = fname + "0";
  fname = fname + hour() + "-";
  if (minute() < 10) fname = fname + "0";
  fname = fname + minute() + "-";
  if (second() < 10) fname = fname + "0";
  fname = fname + second();
  return fname;
}

//these functions are relevant to convertSDFile
public void createPlaybackFileFromSD() {
  logFileName = "data/EEG_Data/SDconverted-"+getDateString()+".txt";
  dataWriter = createWriter(logFileName);
  dataWriter.println("%OBCI Data Log - " + getDateString());
}

public void sdFileSelected(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
  } else {
    println("User selected " + selection.getAbsolutePath());
    dataReader = createReader(selection.getAbsolutePath()); // ("positions.txt");
    controlPanel.convertingSD = true;
    println("Timing SD file conversion...");
    thatTime = millis();
  }
}

//------------------------------------------------------------------------
//                            CLASSES
//------------------------------------------------------------------------

//write data to a text file
public class OutputFile_rawtxt {
  PrintWriter output;
  String fname;
  private int rowsWritten;

  OutputFile_rawtxt(float fs_Hz) {

    //build up the file name
    fname = "SavedData"+System.getProperty("file.separator")+"OpenBCI-RAW-";

    //add year month day to the file name
    fname = fname + year() + "-";
    if (month() < 10) fname=fname+"0";
    fname = fname + month() + "-";
    if (day() < 10) fname = fname + "0";
    fname = fname + day();

    //add hour minute sec to the file name
    fname = fname + "_";
    if (hour() < 10) fname = fname + "0";
    fname = fname + hour() + "-";
    if (minute() < 10) fname = fname + "0";
    fname = fname + minute() + "-";
    if (second() < 10) fname = fname + "0";
    fname = fname + second();

    //add the extension
    fname = fname + ".txt";

    //open the file
    output = createWriter(fname);

    //add the header
    writeHeader(fs_Hz);

    //init the counter
    rowsWritten = 0;
  }

  //variation on constructor to have custom name
  OutputFile_rawtxt(float fs_Hz, String _fileName) {
    fname = "SavedData"+System.getProperty("file.separator")+"OpenBCI-RAW-";
    fname += _fileName;
    fname += ".txt";
    output = createWriter(fname);        //open the file
    writeHeader(fs_Hz);    //add the header
    rowsWritten = 0;    //init the counter
  }

  public void writeHeader(float fs_Hz) {
    output.println("%Darwin Augmented Cognition Raw EEG Data");
    output.println("%");
    output.println("%Sample Rate = " + fs_Hz + " Hz");
    output.println("%First Column = SampleIndex");
    output.println("%Last Column = Timestamp ");
    output.println("%Other Columns = EEG data in microvolts followed by Accel Data (in G) interleaved with Aux Data");
    output.flush();
  }



  public void writeRawData_dataPacket(DataPacket_ADS1299 data, float scale_to_uV, float scale_for_aux) {

    //get current date time with Date()
    Date date = new Date();

    if (output != null) {
      output.print(Integer.toString(data.sampleIndex));
      writeValues(data.values,scale_to_uV);
      writeAccValues(data.auxValues,scale_for_aux);
      output.print( ", " + dateFormat.format(date));
      output.print(", " + Marker_Trigger);
      output.println(); rowsWritten++;
      Marker_Trigger = "";
      //output.flush();
    }
  }

  private void writeValues(int[] values, float scale_fac) {
    int nVal = values.length;
    for (int Ival = 0; Ival < nVal; Ival++) {
      output.print(", ");
      output.print(String.format(Locale.US, "%.2f", scale_fac * PApplet.parseFloat(values[Ival])));
    }
  }

  private void writeAccValues(int[] values, float scale_fac) {
    int nVal = values.length;
    for (int Ival = 0; Ival < nVal; Ival++) {
      output.print(", ");
      output.print(String.format(Locale.US, "%.3f", scale_fac * PApplet.parseFloat(values[Ival])));
    }
  }

  public void closeFile() {
    output.flush();
    output.close();
  }

  public int getRowsWritten() {
    return rowsWritten;
  }
};

//write data to a text file in BDF+ format http://www.biosemi.com/faq/file_format.htm
public class OutputFile_BDF {
  private PrintWriter writer;
  private OutputStream dstream;
  // private FileOutputStream fstream;
  // private BufferedOutputStream bstream;
  // private DataOutputStream dstream;

  // Each header component has a max allocated amount of ascii spaces
  // SPECS FOR BDF http://www.biosemi.com/faq/file_format.htm
  // ADDITIONAL SPECS FOR EDF+ http://www.edfplus.info/specs/edfplus.html#additionalspecs
  // A good resource for a comparison between BDF and EDF http://www.teuniz.net/edfbrowser/bdfplus%20format%20description.html
  final static int BDF_HEADER_SIZE_VERSION = 8; // Version of this data format Byte 1: "255" (non ascii) Bytes 2-8 : "BIOSEMI" (ASCII)
  final static int BDF_HEADER_SIZE_PATIENT_ID = 80; // Local patient identification (mind item 3 of the additional EDF+ specs)
  final static int BDF_HEADER_SIZE_RECORDING_ID = 80; // Local recording identification (mind item 4 of the additional EDF+ specs)
  final static int BDF_HEADER_SIZE_RECORDING_START_DATE = 8; // Start date of recording (dd.mm.yy) (mind item 2 of the additional EDF+ specs)
  final static int BDF_HEADER_SIZE_RECORDING_START_TIME = 8; // Start time of recordign (hh.mm.ss)
  final static int BDF_HEADER_SIZE_BYTES_IN_HEADER = 8; // Number of bytes in header record
  final static int BDF_HEADER_SIZE_RESERVED = 44; // Reserved
  final static int BDF_HEADER_SIZE_NUMBER_DATA_RECORDS = 8; // Number of data records (-1 if unknown, obey item 10 of the additional EDF+ specs)
  final static int BDF_HEADER_SIZE_DURATION_OF_DATA_RECORD = 8; // Duration of a data record, in seconds
  final static int BDF_HEADER_SIZE_NUMBER_SIGNALS = 4; // Number of signals (ns) in data record
  final static int BDF_HEADER_NS_SIZE_LABEL = 16; // ns * 16 ascii : ns * label (e.g. EEG Fpz-Cz or Body temp) (mind item 9 of the additional EDF+ specs)
  final static int BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE = 80; // ns * 80 ascii : ns * transducer type (e.g. AgAgCl electrode)
  final static int BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION = 8; // ns * 8 ascii : ns * physical dimension (e.g. uV or degreeC)
  final static int BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM = 8; // ns * 8 ascii : ns * physical minimum (e.g. -500 or 34)
  final static int BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM = 8; // ns * 8 ascii : ns * physical maximum (e.g. 500 or 40)
  final static int BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM = 8; // ns * 8 ascii : ns * digital minimum (e.g. -2048)
  final static int BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM = 8; // ns * 8 ascii : ns * digital maximum (e.g. 2047)
  final static int BDF_HEADER_NS_SIZE_PREFILTERING = 80; // ns * 80 ascii : ns * prefiltering (e.g. HP:0.1Hz LP:75Hz)
  final static int BDF_HEADER_NS_SIZE_NR = 8; // ns * 8 ascii : ns * nr of samples in each data record
  final static int BDF_HEADER_NS_SIZE_RESERVED = 32; // ns * 32 ascii : ns * reserved

  // Ref: http://www.edfplus.info/specs/edfplus.html#header
  final static String BDF_HEADER_DATA_CONTINUOUS = "BDF+C";
  final static String BDF_HEADER_DATA_DISCONTINUOUS = "BDF+D";
  final static String BDF_HEADER_PHYSICAL_DIMENISION_UV = "uV";
  final static String BDF_HEADER_PHYSICAL_DIMENISION_G = "g";
  final static String BDF_HEADER_TRANSDUCER_AGAGCL = "AgAgCl electrode";
  final static String BDF_HEADER_TRANSDUCER_MEMS = "MEMS";
  final static String BDF_HEADER_ANNOTATIONS = "BDF Annotations ";

  final static int BDF_HEADER_BYTES_BLOCK = 256;

  DateFormat startDateFormat = new SimpleDateFormat("dd.MM.yy");
  DateFormat startTimeFormat = new SimpleDateFormat("hh.mm.ss");

  private char bdf_version_header = 0xFF;
  private char[] bdf_version = {'B', 'I', 'O', 'S', 'E', 'M', 'I'};

  private String bdf_patient_id_subfield_hospoital_code = "X"; // The code by which the patient is known in the hospital administration.
  private String bdf_patient_id_subfield_sex = "X"; // Sex (English, so F or M).
  private String bdf_patient_id_subfield_birthdate = "X"; // (e.g. 24-NOV-1992) Birthdate in dd-MM-yyyy format using the English 3-character abbreviations of the month in capitals. 02-AUG-1951 is OK, while 2-AUG-1951 is not.
  private String bdf_patient_id_subfield_name = "X"; // the patients name. No spaces! Use "_" where ever a space is

  private String bdf_recording_id_subfield_prefix = "X"; //"Startdate"; // The text 'Startdate'
  private String bdf_recording_id_subfield_startdate = "X"; // getDateString(startDateFormat); // The startdate itself in dd-MM-yyyy format using the English 3-character abbreviations of the month in capitals.
  private String bdf_recording_id_subfield_admin_code = "X"; // The hospital administration code of the investigation, i.e. EEG number or PSG number.
  private String bdf_recording_id_subfield_investigator = "X"; // A code specifying the responsible investigator or technician.
  private String bdf_recording_id_subfield_equipment = "X"; // A code specifying the used equipment.

  // Digital max and mins
  private String bdf_digital_minimum_ADC_24bit = "-8388608"; // -1 * 2^23
  private String bdf_digital_maximum_ADC_24bit = "8388607"; // 2^23 - 1
  private String bdf_digital_minimum_ADC_12bit = "-2048"; // -1 * 2^11
  private String bdf_digital_maximum_ADC_12bit = "2047"; // 2^11 - 1

  // Physcial max and mins
  private String bdf_physical_minimum_ADC_24bit = "-187500"; // 4.5 / 24 / (2^23) * 1000000 *  (2^23)
  private String bdf_physical_maximum_ADC_24bit = "187500"; // 4.5 / 24 / (2^23) * 1000000 * -1 * (2^23)
  private String bdf_physical_minimum_ADC_Accel = "-4";
  private String bdf_physical_maximum_ADC_Accel = "4";

  private String bdf_physical_minimum_ADC_24bit_ganglion = "-15686";
  private String bdf_physical_maximum_ADC_24bit_ganglion = "15686";

  private final float ADS1299_Vref = 4.5f;  //reference voltage for ADC in ADS1299.  set by its hardware
  private float ADS1299_gain = 24.0f;  //assumed gain setting for ADS1299.  set by its Arduino code
  private float scale_fac_uVolts_per_count = ADS1299_Vref / ((float)(pow(2,23)-1)) / ADS1299_gain  * 1000000.f; //ADS1299 datasheet Table 7, confirmed through experiment

  private int bdf_number_of_data_records = -1;

  public boolean continuous = true;
  public boolean write_accel = true;

  private float dataRecordDuration = 1; // second
  private int nbAnnotations = 1;
  private int nbAux = 3;
  private int nbChan = 8;
  private int sampleSize = 3; // Number of bytes in a sample

  private String labelsAnnotations[] = new String[nbAnnotations];
  private String transducerAnnotations[] = new String[nbAnnotations];
  private String physicalDimensionAnnotations[] = new String[nbAnnotations];
  private String physicalMinimumAnnotations[] = new String[nbAnnotations];
  private String physicalMaximumAnnotations[] = new String[nbAnnotations];
  private String digitalMinimumAnnotations[] = new String[nbAnnotations];
  private String digitalMaximumAnnotations[] = new String[nbAnnotations];
  private String prefilteringAnnotations[] = new String[nbAnnotations];
  private String nbSamplesPerDataRecordAnnotations[] = new String[nbAnnotations];
  private String reservedAnnotations[] = new String[nbAnnotations];

  private String labelsAux[] = new String[nbAux];
  private String transducerAux[] = new String[nbAux];
  private String physicalDimensionAux[] = new String[nbAux];
  private String physicalMinimumAux[] = new String[nbAux];
  private String physicalMaximumAux[] = new String[nbAux];
  private String digitalMinimumAux[] = new String[nbAux];
  private String digitalMaximumAux[] = new String[nbAux];
  private String prefilteringAux[] = new String[nbAux];
  private String nbSamplesPerDataRecordAux[] = new String[nbAux];
  private String reservedAux[] = new String[nbAux];

  private String labelsEEG[] = new String[nbChan];
  private String transducerEEG[] = new String[nbChan];
  private String physicalDimensionEEG[] = new String[nbChan];
  private String physicalMinimumEEG[] = new String[nbChan];
  private String physicalMaximumEEG[] = new String[nbChan];
  private String digitalMinimumEEG[] = new String[nbChan];
  private String digitalMaximumEEG[] = new String[nbChan];
  private String prefilteringEEG[] = new String[nbChan];
  private String nbSamplesPerDataRecordEEG[] = new String[nbChan];
  private String reservedEEG[] = new String[nbChan];

  private String tempWriterPrefix = "temp.txt";

  private int fs_Hz = 250;
  private int accel_Hz = 25;

  private int samplesInDataRecord = 0;
  private int dataRecordsWritten = 0;

  private Date startTime;
  private boolean startTimeCaptured = false;

  private int timeDataRecordStart = 0;

  private byte auxValBuf[][][];
  private byte auxValBuf_buffer[][][];
  private byte chanValBuf[][][];
  private byte chanValBuf_buffer[][][];

  public String fname = "";

  public int nbSamplesPerAnnontation = 20;

  public DataPacket_ADS1299 data_t;

  /**
   * @description Creates an EDF writer! Name of output file based on current
   *  date and time.
   * @param `_fs_Hz` {float} - The sample rate of the data source. Going to be
   *  `250` for OpenBCI 32bit board, `125` for OpenBCI 32bit board + daisy, or
   *  `256` for the Ganglion.
   * @param `_nbChan` {int} - The number of channels of the data source. Going to be
   *  `8` for OpenBCI 32bit board, `16` for OpenBCI 32bit board + daisy, or
   *  `4` for the Ganglion.
   * @constructor
   */
  OutputFile_BDF(float _fs_Hz, int _nbChan) {

    fname = getFileName();
    fs_Hz = (int)_fs_Hz;
    nbChan = _nbChan;

    init();
  }

  /**
   * @description Creates an EDF writer! The output file will contain the `_filename`.
   * @param `_fs_Hz` {float} - The sample rate of the data source. Going to be
   *  `250` for OpenBCI 32bit board, `125` for OpenBCI 32bit board + daisy, or
   *  `256` for the Ganglion.
   * @param `_nbChan` {int} - The number of channels of the data source. Going to be
   *  `8` for OpenBCI 32bit board, `16` for OpenBCI 32bit board + daisy, or
   *  `4` for the Ganglion.
   * @param `_fileName` {String} - Main component of the output file name.
   * @constructor
   */
  OutputFile_BDF(float _fs_Hz, int _nbChan, String _fileName) {

    fname = getFileName(_fileName);
    fs_Hz = (int)_fs_Hz;
    nbChan = _nbChan;

    init();
  }

  /**
   * @description Used to initalize the writer.
   */
  private void init() {

    // Set the arrays needed for header
    setNbAnnotations(nbAnnotations);
    setNbAux(nbAux);
    setNbChan(nbChan);

    // Create the aux value buffer
    auxValBuf = new byte[nbAux][fs_Hz][sampleSize];
    auxValBuf_buffer = new byte[nbAux][fs_Hz][sampleSize];

    // Create the channel value buffer
    chanValBuf = new byte[nbChan][fs_Hz][sampleSize];
    chanValBuf_buffer = new byte[nbChan][fs_Hz][sampleSize];

    // Create the output stream for raw data
    dstream = createOutput(tempWriterPrefix);

    // Init the counter
    dataRecordsWritten = 0;
  }

  /**
   * @description Writes a raw data packet to the buffer. Also will flush the
   *  buffer if it is filled with one second worth of data. Will also capture
   *  the start time, or the first time a packet is recieved.
   * @param `data` {DataPacket_ADS1299} - A data packet
   */
  public void writeRawData_dataPacket(DataPacket_ADS1299 data) {

    if (!startTimeCaptured) {
      startTime = new Date();
      startTimeCaptured = true;
      timeDataRecordStart = millis();
    }

    writeChannelDataValues(data.rawValues);
    if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
      writeAuxDataValues(data.rawAuxValues);
    }
    samplesInDataRecord++;
    // writeValues(data.auxValues,scale_for_aux);
    if (samplesInDataRecord >= fs_Hz) {
      arrayCopy(chanValBuf,chanValBuf_buffer);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
        arrayCopy(auxValBuf,auxValBuf_buffer);
      }

      samplesInDataRecord = 0;
      writeDataOut();
    }
  }

  private void writeDataOut() {
    try {
      for (int i = 0; i < nbChan; i++) {
        for (int j = 0; j < fs_Hz; j++) {
          for (int k = 0; k < 3; k++) {
            dstream.write(chanValBuf_buffer[i][j][k]);
          }
        }
      }
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
        for (int i = 0; i < nbAux; i++) {
          for (int j = 0; j < fs_Hz; j++) {
            for (int k = 0; k < 3; k++) {
              dstream.write(auxValBuf_buffer[i][j][k]);
            }
          }
        }
      }

      // Write the annotations
      dstream.write('+');
      String _t = str((millis() - timeDataRecordStart) / 1000);
      int strLen = _t.length();
      for (int i = 0; i < strLen; i++) {
        dstream.write(_t.charAt(i));
      }
      dstream.write(20);
      dstream.write(20);
      int lenWritten = 1 + strLen + 1 + 1;
      // for (int i = lenWritten; i < fs_Hz * sampleSize; i++) {
      for (int i = lenWritten; i < nbSamplesPerAnnontation * sampleSize; i++) {
        dstream.write(0);
      }
      dataRecordsWritten++;

    } catch (Exception e) {
      println("writeRawData_dataPacket: Exception ");
      e.printStackTrace();
    }
  }

  public void closeFile() {

    output("Closed the temp data file. Now opening a new file");
    try {
      dstream.close();
    } catch (Exception e) {
      println("closeFile: dstream close exception ");
      e.printStackTrace();
    }
    println("closeFile: started...");
    // File f = new File(fname);
    // fstream = new FileOutputStream(f);
    // bstream = new BufferedOutputStream(fstream);
    // dstream = new DataOutputStream(bstream);

    OutputStream o = createOutput(fname);
    println("closeFile: made file");

    // Create a new writer with the same file name
    // Write the header
    writeHeader(o);
    output("Header writen, now writing data.");
    println("closeFile: wrote header");

    writeData(o);
    output("Data written. Closing new file.");
    println("closeFile: wrote data");
    // Create write stream
    // try {
    //   println("closeFile: started...");
    //   // File f = new File(fname);
    //   // fstream = new FileOutputStream(f);
    //   // bstream = new BufferedOutputStream(fstream);
    //   // dstream = new DataOutputStream(bstream);
    //
    //   OutputStream o = createOutput(fname);
    //   println("closeFile: made file");
    //
    //   // Create a new writer with the same file name
    //   // Write the header
    //   writeHeader(o);
    //   output("Header writen, now writing data.");
    //   println("closeFile: wrote header");
    //
    //   writeData(o);
    //   output("Data written. Closing new file.");
    //   println("closeFile: wrote data");
    //
    //   // dstream.close();
    //
    //   // Try to delete the file
    //   // https://forum.processing.org/one/topic/noob-how-to-delete-a-file-in-the-data-folder.html
    //   // File f = new File(tempWriterPrefix);
    //   // if (f.exists()) {
    //   //   f.delete();
    //   //   output("Deleted temp data file.");
    //   // } else {
    //   //   output("Unable to delete temp data file.");
    //   // }
    // }
    // catch(IOException e) {
    //   println("closeFile: IOException");
    //   e.printStackTrace();
    // }

  }

  public int getRecordsWritten() {
    return dataRecordsWritten;
  }

  /**
   * @description Resizes and resets the per aux channel arrays to size `n`
   * @param `n` {int} - The new size of arrays
   */
  public void setAnnotationsArraysToSize(int n) {
    labelsAnnotations = new String[n];
    transducerAnnotations = new String[n];
    physicalDimensionAnnotations = new String[n];
    physicalMinimumAnnotations = new String[n];
    physicalMaximumAnnotations = new String[n];
    digitalMinimumAnnotations = new String[n];
    digitalMaximumAnnotations = new String[n];
    prefilteringAnnotations = new String[n];
    nbSamplesPerDataRecordAnnotations = new String[n];
    reservedAnnotations = new String[n];
  }

  /**
   * @description Resizes and resets the per aux channel arrays to size `n`
   * @param `n` {int} - The new size of arrays
   */
  public void setAuxArraysToSize(int n) {
    labelsAux = new String[n];
    transducerAux = new String[n];
    physicalDimensionAux = new String[n];
    physicalMinimumAux = new String[n];
    physicalMaximumAux = new String[n];
    digitalMinimumAux = new String[n];
    digitalMaximumAux = new String[n];
    prefilteringAux = new String[n];
    nbSamplesPerDataRecordAux = new String[n];
    reservedAux = new String[n];
  }

  /**
   * @description Resizes and resets the per channel arrays to size `n`
   * @param `n` {int} - The new size of arrays
   */
  public void setEEGArraysToSize(int n) {
    labelsEEG = new String[n];
    transducerEEG = new String[n];
    physicalDimensionEEG = new String[n];
    physicalMinimumEEG = new String[n];
    physicalMaximumEEG = new String[n];
    digitalMinimumEEG = new String[n];
    digitalMaximumEEG = new String[n];
    prefilteringEEG = new String[n];
    nbSamplesPerDataRecordEEG = new String[n];
    reservedEEG = new String[n];
  }

  /**
   * @description Set an EEG 10-20 label for a given channel. (e.g. EEG Fpz-Cz)
   * @param `s` {String} - The string to store to the `labels` string array
   * @param `index` {int} - The position in the `labels` array to insert the
   *  string `str`. Must be smaller than `nbChan`.
   * @returns {boolean} - `true` if the label was added, `false` if not able to
   */
  public boolean setEEGLabelForIndex(String s, int index) {
    if (index < nbChan) {
      labelsEEG[index] = s;
      return true;
    } else {
      return false;
    }
  }

  /**
   * @description Set the number of annotation signals.
   * @param `n` {int} - The new number of channels
   */
  public void setNbAnnotations(int n) {
    if (n < 1) n = 1;

    // Set the main variable
    nbAnnotations = n;
    // Resize the arrays
    setAnnotationsArraysToSize(n);
    // Fill any arrays that can be filled
    setAnnotationsArraysToDefaults();
  }

  /**
   * @description Set the number of aux signals.
   * @param `n` {int} - The new number of aux channels
   */
  public void setNbAux(int n) {
    if (n < 1) n = 1;

    // Set the main variable
    nbAux = n;
    // Resize the arrays
    setAuxArraysToSize(n);
    // Fill any arrays that can be filled
    setAuxArraysToDefaults();
  }

  /**
   * @description Set the number of channels. Important to do. This will nuke
   *  the labels array if the size increases or decreases.
   * @param `n` {int} - The new number of channels
   */
  public void setNbChan(int n) {
    if (n < 1) n = 1;

    // Set the main variable
    nbChan = n;
    // Resize the arrays
    setEEGArraysToSize(n);
    // Fill any arrays that can be filled
    setEEGArraysToDefaults();
  }

  /**
   * @description Sets the patient's sex.
   * @param `s` {String} - The patients sex (e.g. M or F)
   * @returns {String} - The string that was set.
   */
  public String setPatientIdSex(String s) {
    return bdf_patient_id_subfield_sex = swapSpacesForUnderscores(s);
  }

  /**
   * @description Sets the patient's birthdate.
   * @param `s` {String} - The patients birth date (e.g. 24-NOV-1992)
   * @returns {String} - The string that was set.
   */
  public String setPatientIdBirthdate(String s) {
    return bdf_patient_id_subfield_birthdate = swapSpacesForUnderscores(s);
  }

  /**
   * @description Sets the patient's name. Note that spaces will be swapped for
   *  underscores.
   * @param `s` {String} - The patients name.
   * @returns {String} - The string that was set.
   */
  public String setPatientIdName(String s) {
    return bdf_patient_id_subfield_name = swapSpacesForUnderscores(s);
  }

  /**
   * @description Set any prefilerting for a given channel. (e.g. HP:0.1Hz LP:75Hz)
   * @param `s` {String} - The string to store to the `prefiltering` string array
   * @param `index` {int} - The position in the `prefiltering` array to insert the
   *  string `str`. Must be smaller than `nbChan`.
   * @returns {boolean} - `true` if the string was added, `false` if not able to
   */
  public boolean setEEGPrefilterForIndex(String s, int index) {
    if (index < nbChan) {
      prefilteringEEG[index] = s;
      return true;
    } else {
      return false;
    }
  }

  /**
   * @description Sets the recording admin code. Note that spaces will be
   *  swapped for underscores.
   * @param `s` {String} - The recording admin code.
   * @returns {String} - The string that was set.
   */
  public String setRecordingIdAdminCode(String s) {
    return bdf_recording_id_subfield_admin_code = swapSpacesForUnderscores(s);
  }

  /**
   * @description Sets the recording admin code. Note that spaces will be
   *  swapped for underscores. (e.g. AJ Keller)
   * @param `s` {String} - The recording id of the investigator.
   * @returns {String} - The string that was set. (e.g. AJ_Keller)
   */
  public String setRecordingIdInvestigator(String s) {
    return bdf_recording_id_subfield_investigator = swapSpacesForUnderscores(s);
  }

  /**
   * @description Sets the recording equipment code. Note that spaces will be
   *  swapped for underscores. (e.g. OpenBCI 32bit or OpenBCI Ganglion)
   * @param `s` {String} - The recording equipment id.
   * @returns {String} - The string that was set.
   */
  public String setRecordingIdEquipment(String s) {
    return bdf_recording_id_subfield_equipment = swapSpacesForUnderscores(s);
  }

  /**
   * @description Set a transducer type for a given channel. (e.g. AgAgCl electrode)
   * @param `s` {String} - The string to store to the `transducerEEG` string array
   * @param `index` {int} - The position in the `transducerEEG` array to insert the
   *  string `str`. Must be smaller than `nbChan`.
   * @returns {boolean} - `true` if the string was added, `false` if not able to
   */
  public boolean setTransducerForIndex(String s, int index) {
    if (index < nbChan) {
      transducerEEG[index] = s;
      return true;
    } else {
      return false;
    }
  }

  /**
   * @description Used to combine a `str` (string) into one big string a certain number of
   *  `times` with left justification padding of `size`.
   * @param `s` {String} - The string to be inserted
   * @param `size` {int} - The total allowable size for `str` to be inserted.
   *  If `str.length()` < `size` then `str` will essentially be right padded with
   *  spaces till the `output` is of length `size`.
   * @param `times` {int} - The number of times to repeat the `str` with `padding`
   * @returns {String} - The `str` right padded with spaces to beome `size` length
   *  and that repeated `times`.
   */
  private String combineStringIntoSizeTimes(String s, int size, int times) {
    String output = "";
    for (int i = 0; i < times; i++) {
      output += padStringRight(s, size);
    }
    return output;
  }

  /**
   * @description Calculate the number of bytes in the header. Entirerly based
   *  off the number of channels (`nbChan`)
   * @returns {int} - The number of bytes in the header is 256 + (256 * N) where
   *  N is the number of channels (signals)
   */
  private int getBytesInHeader() {
    return BDF_HEADER_BYTES_BLOCK + (BDF_HEADER_BYTES_BLOCK * getNbSignals()); // Add one for the annotations channel
  }

  /**
   * @description Used to get the continuity of the EDF file based on class public
   *  boolean variable `continuous`. If stop stream then start stream is pressed
   *  we must set the variable `continuous` to false.
   * @returns {String} - The string with NO spacing
   */
  private String getContinuity() {
    if (continuous) {
      return BDF_HEADER_DATA_CONTINUOUS;
    } else {
      return BDF_HEADER_DATA_DISCONTINUOUS;
    }
  }

  /**
   * @description Returns a string of the date based on the input DateFormat `d`
   * @param `d` {DateFormat} - The format you want the date/time in
   * @returns {String} - The current date/time formatted based on `d`
   */
  private String getDateString(DateFormat d) {
    // Get current date time with Date()
    return d.format(new Date());
  }

  /**
   * @description Returns a string of the date based on the input DateFormat `d`
   * @param `d` {DateFormat} - The format you want the date/time in
   * @returns {String} - The current date/time formatted based on `d`
   */
  private String getDateString(Date d, DateFormat df) {
    // Get current date time with Date()
    return df.format(d);
  }

  /**
   * @description Generate a file name for the EDF file that has the current date
   *  and time injected into it.
   * @returns {String} - A fully qualified name of an output file with the date
   *  and time.
   */
  private String getFileName() {
    //build up the file name
    String output = "";

    // If no file name is supplied then we generate one based off the current
    //  date and time of day.
    output += year() + "-";
    if (month() < 10) output += "0";
    output += month() + "-";
    if (day() < 10) output += "0";
    output += day();

    output += "_";
    if (hour() < 10) output += "0";
    output += hour() + "-";
    if (minute() < 10) output += "0";
    output += minute() + "-";
    if (second() < 10) output += "0";
    output += second();

    return getFileName(output);
  }

  /**
   * @description Generate a file name for the EDF file with `str` string embedded
   *  within.
   * @param `s` {String} - The string to inject
   * @returns {String} - A fully qualified name of an output file with `str`.
   */
  private String getFileName(String s) {
    String output = "SavedData"+System.getProperty("file.separator")+"OpenBCI-EDF-";
    output += s;
    output += ".edf";
    return output;
  }

  /**
   * @description Get's the number of signal channels to write out. Have to
   *  keep in mind that the annotations channel counts.
   * @returns {int} - The number of signals in the header.
   */
  private int getNbSignals() {
    if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
      return nbChan + nbAux + nbAnnotations;
    } else {
      return nbChan + nbAnnotations;
    }

  }

  /**
   * @description Takes an array of strings and joins split by `delimiter`
   * @param `stringArray` {String []} - An array of strings
   * @param `delimiter` {String} - The delimiter to split the strings with
   * @returns `String` - All the strings from `stringArray` separated by
   *  `delimiter`.
   * @reference http://www.edfplus.info/specs/edf.html
   */
  private String joinStringArray(String[] stringArray, String delimiter) {
    String output = "";

    // Number of elecments to add
    int numberOfElements = stringArray.length;

    // Each element will be written
    for (int i = 0; i < numberOfElements; i++) {
      // Add the element
      output += stringArray[i];
      // Add a delimiter between
      output += delimiter;
    }

    return output;
  }

  /**
   * @description Used to combine a `str` (string) with left justification padding of `size`.
   * @param `s` {String} - The string to be inserted
   * @param `size` {int} - The total allowable size for `str` to be inserted.
   *  If `str.length()` < `size` then `str` will essentially be right padded with
   *  spaces till the `output` is of length `size`.
   * @returns {String} - The `str` right padded with spaces to become `size` length.
   */
  private String padStringRight(String s, int size) {
    char[] output = new char[size];
    int len = 0;
    if (s != null) len = s.length();
    for (int i = 0; i < size; i++) {
      if (i < len) {
        output[i] = s.charAt(i);
      } else {
        output[i] = ' ';
      }
    }
    return new String(output, 0, size);
  }

  /**
   * @description Sets the header per channel arrays to their default values
   */
  private void setAuxArraysToDefaults() {
    labelsAux[0] = "Accel X";
    labelsAux[1] = "Accel Y";
    labelsAux[2] = "Accel Z";
    setStringArray(transducerAux, BDF_HEADER_TRANSDUCER_MEMS, nbAux);
    setStringArray(physicalDimensionAux, BDF_HEADER_PHYSICAL_DIMENISION_G, nbAux);
    setStringArray(digitalMinimumAux, bdf_digital_minimum_ADC_12bit, nbAux);
    setStringArray(digitalMaximumAux, bdf_digital_maximum_ADC_12bit, nbAux);
    setStringArray(physicalMinimumAux, bdf_physical_minimum_ADC_Accel, nbAux);
    setStringArray(physicalMaximumAux, bdf_physical_maximum_ADC_Accel, nbAux);
    setStringArray(prefilteringAux, " ", nbAux);
    setStringArray(nbSamplesPerDataRecordAux, str(fs_Hz), nbAux);
    setStringArray(reservedAux, " ", nbAux);
  }

  /**
   * @description Sets the header per channel arrays to their default values
   */
  private void setAnnotationsArraysToDefaults() {
    setStringArray(labelsAnnotations, BDF_HEADER_ANNOTATIONS, 1); // Leave space for the annotations space
    setStringArray(transducerAnnotations, " ", 1);
    setStringArray(physicalDimensionAnnotations, " ", 1);
    setStringArray(digitalMinimumAnnotations, bdf_digital_minimum_ADC_24bit, 1);
    setStringArray(digitalMaximumAnnotations, bdf_digital_maximum_ADC_24bit, 1);
    if (eegDataSource == DATASOURCE_GANGLION) {
      setStringArray(physicalMinimumAnnotations, bdf_physical_minimum_ADC_24bit_ganglion, 1);
      setStringArray(physicalMaximumAnnotations, bdf_physical_maximum_ADC_24bit_ganglion, 1);
    } else {
      setStringArray(physicalMinimumAnnotations, bdf_physical_minimum_ADC_24bit, 1);
      setStringArray(physicalMaximumAnnotations, bdf_physical_maximum_ADC_24bit, 1);
    }
    setStringArray(prefilteringAnnotations, " ", 1);
    nbSamplesPerDataRecordAnnotations[0] = str(nbSamplesPerAnnontation);
    setStringArray(reservedAnnotations, " ", 1);
  }

  /**
   * @description Sets the header per channel arrays to their default values
   */
  private void setEEGArraysToDefaults() {
    for (int i = 1; i <= nbChan; i++) {
      labelsEEG[i - 1] = "EEG " + i;
    }
    setStringArray(transducerEEG, BDF_HEADER_TRANSDUCER_AGAGCL, nbChan);
    setStringArray(physicalDimensionEEG, BDF_HEADER_PHYSICAL_DIMENISION_UV, nbChan);
    setStringArray(digitalMinimumEEG, bdf_digital_minimum_ADC_24bit, nbChan);
    setStringArray(digitalMaximumEEG, bdf_digital_maximum_ADC_24bit, nbChan);
    setStringArray(physicalMinimumEEG, bdf_physical_minimum_ADC_24bit, nbChan);
    setStringArray(physicalMaximumEEG, bdf_physical_maximum_ADC_24bit, nbChan);
    setStringArray(prefilteringEEG, " ", nbChan);
    setStringArray(nbSamplesPerDataRecordEEG, str(fs_Hz), nbChan);
    setStringArray(reservedEEG, " ", nbChan);
  }

  /**
   * @description Convience function to fill a string array with the same values
   * @param `arr` {String []} - A string array to fill
   * @param `val` {Stirng} - The string to be inserted into `arr`
   */
  private void setStringArray(String[] arr, String val, int len) {
    for (int i = 0; i < len; i++) {
      arr[i] = val;
    }
  }

  /**
   * @description Converts a byte from Big Endian to Little Endian
   * @param `val` {byte} - The byte to swap
   * @returns {byte} - The swapped byte.
   */
  private byte swapByte(byte val) {
    int mask = 0x80;
    int res = 0;
    // println("swapByte: starting to swap val: 0b" + binary(val,8));
    for (int i = 0; i < 8; i++) {
      // println("\nswapByte: i: " + i);
      // Isolate the MSB with a big mask i.e. 10000000, 01000000, etc...
      int temp = (val & mask);
      // println("swapByte: temp:    0b" + binary(temp,8));
      // Save this temp value
      res = (res >> 1) | (temp << i);
      // println("swapByte: res:     0b" + binary(res,8));
      // Move mask one place
      mask = mask >> 1;
      // println("swapByte: mask: 0b" + binary(mask,32));
    }
    // println("swapByte: ending swapped val: 0b" + binary(res,8));
    return (byte)res;
  }

  /**
   * @description Swaps any spaces for underscores because EDF+ calls for it
   * @param `s` {String} - A string containing spaces
   * @returns {String} - A string with underscores instead of spaces.
   * @reference http://www.edfplus.info/specs/edfplus.html#additionalspecs
   */
  private String swapSpacesForUnderscores(String s) {
    int len = s.length();
    char[] output = new char[len];
    // Loop through the String
    for (int i = 0; i < len; i++) {
      if (s.charAt(i) == ' ') {
        output[i] = '_';
      } else {
        output[i] = s.charAt(i);
      }
    }
    return new String(output, 0, len);
  }

  /**
   * @description Moves a packet worth of data into channel buffer, also converts
   *  from Big Endian to Little Indian as per the specs of BDF+.
   *  Ref [1]: http://www.biosemi.com/faq/file_format.htm
   * @param `values` {byte[][]} - A byte array that is n_chan X sample size (3)
   */
  private void writeChannelDataValues(byte[][] values) {
    for (int i = 0; i < nbChan; i++) {
      // Make the values little endian
      chanValBuf[i][samplesInDataRecord][0] = swapByte(values[i][2]);
      chanValBuf[i][samplesInDataRecord][1] = swapByte(values[i][1]);
      chanValBuf[i][samplesInDataRecord][2] = swapByte(values[i][0]);
    }
  }

  /**
   * @description Moves a packet worth of data into aux buffer, also converts
   *  from Big Endian to Little Indian as per the specs of BDF+.
   *  Ref [1]: http://www.biosemi.com/faq/file_format.htm
   * @param `values` {byte[][]} - A byte array that is n_aux X sample size (3)
   */
  private void writeAuxDataValues(byte[][] values) {
    for (int i = 0; i < nbAux; i++) {
      if (write_accel) {
        // grab the lower part of
        boolean zeroPack = true;
        // shift right
        int t = (int)values[i][0] & 0x0F;
        values[i][0] = (byte)((int)values[i][0] >> 4);
        if (values[i][0] >= 8) {
          zeroPack = false;
        }
        values[i][1] = (byte)((int)values[i][1] >> 4);
        values[i][1] = (byte)((int)values[i][1] | t);
        if (!zeroPack) {
          values[i][0] = (byte)((int)values[i][0] | 0xF0);
        }
        // make msb -> lsb
        auxValBuf[i][samplesInDataRecord][0] = swapByte(values[i][1]);
        auxValBuf[i][samplesInDataRecord][1] = swapByte(values[i][0]);
        // pad byte
        if (zeroPack) {
          auxValBuf[i][samplesInDataRecord][2] = (byte)0x00;
        } else {
          auxValBuf[i][samplesInDataRecord][2] = (byte)0xFF;
        }
      } else {
        // TODO: Implement once GUI gets support for non standard packets
      }
    }
  }

  /**
   * @description Writes data from a temp file over to the final file with the
   *  header in place already.
   *  TODO: Stop keeping it in memory.
   * @param `o` {OutputStream} - An output stream to write to.
   */
  private void writeData(OutputStream o) {

    InputStream input = createInput(tempWriterPrefix);

    try {
      println("writeData: started...");
      int data = input.read();
      int byteCount = 0;
      while (data != -1) {
        o.write(data);
        data = input.read();
        byteCount++;
      }
      println("writeData: finished: wrote " + byteCount + " bytes");
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        input.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @description Writes a fully qualified BDF+ header
   */
  private void writeHeader(OutputStream o) {
    // writer.write(0xFF); // Write the first byte of the header here
    try {
      // println("writeHeader: starting...");

      o.write(0xFF);
      writeString(padStringRight(new String(bdf_version),BDF_HEADER_SIZE_VERSION - 1), o); // Do one less then supposed to because of the first byte already written.
      String[] temp1  = {bdf_patient_id_subfield_hospoital_code,bdf_patient_id_subfield_sex,bdf_patient_id_subfield_birthdate,bdf_patient_id_subfield_name};
      writeString(padStringRight(joinStringArray(temp1, " "), BDF_HEADER_SIZE_PATIENT_ID), o);
      String[] temp2 = {bdf_recording_id_subfield_prefix,bdf_recording_id_subfield_startdate,bdf_recording_id_subfield_admin_code,bdf_recording_id_subfield_investigator,bdf_recording_id_subfield_equipment};
      writeString(padStringRight(joinStringArray(temp2, " "), BDF_HEADER_SIZE_RECORDING_ID), o);
      writeString(getDateString(startTime, startDateFormat), o);
      writeString(getDateString(startTime, startTimeFormat), o);
      writeString(padStringRight(str(getBytesInHeader()),BDF_HEADER_SIZE_BYTES_IN_HEADER), o);
      writeString(padStringRight("24BIT",BDF_HEADER_SIZE_RESERVED), o);//getContinuity(),BDF_HEADER_SIZE_RESERVED), o);
      writeString(padStringRight(str(dataRecordsWritten),BDF_HEADER_SIZE_NUMBER_DATA_RECORDS), o);
      writeString(padStringRight("1",BDF_HEADER_SIZE_DURATION_OF_DATA_RECORD), o);
      writeString(padStringRight(str(getNbSignals()),BDF_HEADER_SIZE_NUMBER_SIGNALS), o);

      writeStringArrayWithPaddingTimes(labelsEEG, BDF_HEADER_NS_SIZE_LABEL, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(labelsAux, BDF_HEADER_NS_SIZE_LABEL, o);
      writeStringArrayWithPaddingTimes(labelsAnnotations, BDF_HEADER_NS_SIZE_LABEL, o);

      writeStringArrayWithPaddingTimes(transducerEEG, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(transducerAux, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);
      writeStringArrayWithPaddingTimes(transducerAnnotations, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);

      writeStringArrayWithPaddingTimes(physicalDimensionEEG, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(physicalDimensionAux, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);
      writeStringArrayWithPaddingTimes(physicalDimensionAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);

      writeStringArrayWithPaddingTimes(physicalMinimumEEG, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(physicalMinimumAux, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);
      writeStringArrayWithPaddingTimes(physicalMinimumAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);

      writeStringArrayWithPaddingTimes(physicalMaximumEEG, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(physicalMaximumAux, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);
      writeStringArrayWithPaddingTimes(physicalMaximumAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);

      writeStringArrayWithPaddingTimes(digitalMinimumEEG, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(digitalMinimumAux, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);
      writeStringArrayWithPaddingTimes(digitalMinimumAnnotations, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);

      writeStringArrayWithPaddingTimes(digitalMaximumEEG, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(digitalMaximumAux, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);
      writeStringArrayWithPaddingTimes(digitalMaximumAnnotations, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);

      writeStringArrayWithPaddingTimes(prefilteringEEG, BDF_HEADER_NS_SIZE_PREFILTERING, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(prefilteringAux, BDF_HEADER_NS_SIZE_PREFILTERING, o);
      writeStringArrayWithPaddingTimes(prefilteringAnnotations, BDF_HEADER_NS_SIZE_PREFILTERING, o);

      writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordEEG, BDF_HEADER_NS_SIZE_NR, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordAux, BDF_HEADER_NS_SIZE_NR, o);
      writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordAnnotations, BDF_HEADER_NS_SIZE_NR, o);

      writeStringArrayWithPaddingTimes(reservedEEG, BDF_HEADER_NS_SIZE_RESERVED, o);
      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) writeStringArrayWithPaddingTimes(reservedAux, BDF_HEADER_NS_SIZE_RESERVED, o);
      writeStringArrayWithPaddingTimes(reservedAnnotations, BDF_HEADER_NS_SIZE_RESERVED, o);

      // println("writeHeader: done...");

    } catch(Exception e) {
      println("writeHeader: Exception " + e);
    }
  }

  /**
   * @description Write out an array of strings with `padding` on each element.
   *  Each element is padded right.
   * @param `arr` {String []} - An array of strings to write out
   * @param `padding` {int} - The amount of padding for each `arr` element.
   * @param `o` {OutputStream} - The output stream to write to.
   */
  private void writeStringArrayWithPaddingTimes(String[] arr, int padding, OutputStream o) {
    int len = arr.length;
    for (int i = 0; i < len; i++) {
      writeString(padStringRight(arr[i], padding), o);
    }
  }

  /**
   * @description Writes a string to an OutputStream s
   * @param `s` {String} - The string to write.
   * @param `o` {OutputStream} - The output stream to write to.
   */
  private void writeString(String s, OutputStream o) {
    int len = s.length();
    try {
      for (int i = 0; i < len; i++) {
        o.write((int)s.charAt(i));
      }
    } catch (Exception e) {
      println("writeString: exception: " + e);
    }
  }

};

///////////////////////////////////////////////////////////////
//
// Class: Table_CSV
// Purpose: Extend the Table class to handle data files with comment lines
// Created: Chip Audette  May 2, 2014
//
// Usage: Only invoke this object when you want to read in a data
//    file in CSV format.  Read it in at the time of creation via
//
//    String fname = "myfile.csv";
//    TableCSV myTable = new TableCSV(fname);
//
///////////////////////////////////////////////////////////////

class Table_CSV extends Table {
  Table_CSV(String fname) throws IOException {
    init();
    readCSV(PApplet.createReader(createInput(fname)));
  }

  //this function is nearly completely copied from parseBasic from Table.java
  public void readCSV(BufferedReader reader) throws IOException {
    boolean header=false;  //added by Chip, May 2, 2014;
    boolean tsv = false;  //added by Chip, May 2, 2014;

    String line = null;
    int row = 0;
    if (rowCount == 0) {
      setRowCount(10);
    }
    //int prev = 0;  //-1;
    try {
      while ( (line = reader.readLine ()) != null) {
        //added by Chip, May 2, 2014 to ignore lines that are comments
        if (line.charAt(0) == '%') {
          //println("Table_CSV: readCSV: ignoring commented line...");
          continue;
        }

        if (row == getRowCount()) {
          setRowCount(row << 1);
        }
        if (row == 0 && header) {
          setColumnTitles(tsv ? PApplet.split(line, '\t') : split(line,','));
          header = false;
        }
        else {
          setRow(row, tsv ? PApplet.split(line, '\t') : split(line,','));
          row++;
        }

        // this is problematic unless we're going to calculate rowCount first
        if (row % 10000 == 0) {
          /*
        if (row < rowCount) {
           int pct = (100 * row) / rowCount;
           if (pct != prev) {  // also prevents "0%" from showing up
           System.out.println(pct + "%");
           prev = pct;
           }
           }
           */
          try {
            // Sleep this thread so that the GC can catch up
            Thread.sleep(10);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Error reading table on line " + row, e);
    }
    // shorten or lengthen based on what's left
    if (row != getRowCount()) {
      setRowCount(row);
    }
  }
}

//////////////////////////////////
//
//    This collection of functions/methods - convertSDFile, createPlaybackFileFromSD, & sdFileSelected - contains code
//    used to convert HEX files (stored by OpenBCI on the local SD) into text files that can be used for PLAYBACK mode.
//    Created: Conor Russomanno - 10/22/14 (based on code written by Joel Murphy summer 2014)
//
//////////////////////////////////

//variables for SD file conversion
BufferedReader dataReader;
String dataLine;
PrintWriter dataWriter;
String convertedLine;
String thisLine;
String h;
float[] intData = new float[20];
String logFileName;
long thisTime;
long thatTime;

public void convertSDFile() {
  println("");
  try {
    dataLine = dataReader.readLine();
  }
  catch (IOException e) {
    e.printStackTrace();
    dataLine = null;
  }

  if (dataLine == null) {
    // Stop reading because of an error or file is empty
    thisTime = millis() - thatTime;
    controlPanel.convertingSD = false;
    println("nothing left in file");
    println("SD file conversion took "+thisTime+" mS");
    dataWriter.flush();
    dataWriter.close();
  } else {
    //        println(dataLine);
    String[] hexNums = splitTokens(dataLine, ",");

    if (hexNums[0].charAt(0) == '%') {
      //          println(dataLine);
      dataWriter.println(dataLine);
      println(dataLine);
    } else {
      for (int i=0; i<hexNums.length; i++) {
        h = hexNums[i];
        if (i > 0) {
          if (h.charAt(0) > '7') {  // if the number is negative
            h = "FF" + hexNums[i];   // keep it negative
          } else {                  // if the number is positive
            h = "00" + hexNums[i];   // keep it positive
          }
          if (i > 8) { // accelerometer data needs another byte
            if (h.charAt(0) == 'F') {
              h = "FF" + h;
            } else {
              h = "00" + h;
            }
          }
        }
        // println(h); // use for debugging
        if (h.length()%2 == 0) {  // make sure this is a real number
          intData[i] = unhex(h);
        } else {
          intData[i] = 0;
        }

        //if not first column(sample #) or columns 9-11 (accelerometer), convert to uV
        if (i>=1 && i<=8) {
          intData[i] *= openBCI.get_scale_fac_uVolts_per_count();
        }

        //print the current channel value
        dataWriter.print(intData[i]);
        if (i < hexNums.length-1) {
          //print "," separator
          dataWriter.print(",");
        }
      }
      //println();
      dataWriter.println();
    }
  }
}

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------
 //for FFT

DataProcessing dataProcessing;
String curTimestamp;
boolean hasRepeated = false;
HashMap<String,float[][]> processed_file;
HashMap<Integer,String> index_of_times;
HashMap<String,Integer> index_of_times_rev;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//called from systemUpdate when mode=10 and isRunning = true
public void process_input_file() throws Exception {
  processed_file = new HashMap<String, float[][]>();
  index_of_times = new HashMap<Integer, String>();
  index_of_times_rev = new HashMap<String, Integer>();
  float localLittleBuff[][] = new float[nchan][nPointsPerUpdate];

  try {
    while (!hasRepeated) {
      currentTableRowIndex=getPlaybackDataFromTable(playbackData_table, currentTableRowIndex, openBCI.get_scale_fac_uVolts_per_count(), openBCI.get_scale_fac_accel_G_per_count(), dataPacketBuff[lastReadDataPacketInd]);

      for (int Ichan=0; Ichan < nchan; Ichan++) {
        //scale the data into engineering units..."microvolts"
        localLittleBuff[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan]* openBCI.get_scale_fac_uVolts_per_count();
      }
      processed_file.put(curTimestamp, localLittleBuff);
      index_of_times.put(indices,curTimestamp);
      index_of_times_rev.put(curTimestamp,indices);
      indices++;
    }
  }
  catch (Exception e) {
    throw new Exception();
  }

  println("Finished filling hashmap");
  has_processed = true;
}


/*************************/
public int getDataIfAvailable(int pointCounter) {

  if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
    //get data from serial port as it streams in
    //next, gather any new data into the "little buffer"
    while ( (curDataPacketInd != lastReadDataPacketInd) && (pointCounter < nPointsPerUpdate)) {
      lastReadDataPacketInd = (lastReadDataPacketInd+1) % dataPacketBuff.length;  //increment to read the next packet
      for (int Ichan=0; Ichan < nchan; Ichan++) {   //loop over each cahnnel
        //scale the data into engineering units ("microvolts") and save to the "little buffer"
        yLittleBuff_uV[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan] * openBCI.get_scale_fac_uVolts_per_count();
      }
      for (int auxChan=0; auxChan < 3; auxChan++) auxBuff[auxChan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].auxValues[auxChan];
      pointCounter++; //increment counter for "little buffer"
    }
  } else if (eegDataSource == DATASOURCE_GANGLION) {
    //get data from ble as it streams in
    //next, gather any new data into the "little buffer"
    while ( (curDataPacketInd != lastReadDataPacketInd) && (pointCounter < nPointsPerUpdate)) {
      lastReadDataPacketInd = (lastReadDataPacketInd + 1) % dataPacketBuff.length;  //increment to read the next packet
      for (int Ichan=0; Ichan < nchan; Ichan++) {   //loop over each cahnnel
        //scale the data into engineering units ("microvolts") and save to the "little buffer"
        yLittleBuff_uV[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan] * ganglion.get_scale_fac_uVolts_per_count();
      }
      pointCounter++; //increment counter for "little buffer"
    }

  } else {
    // make or load data to simulate real time

    //has enough time passed?
    int current_millis = millis();
    if (current_millis >= nextPlayback_millis) {
      //prepare for next time
      int increment_millis = PApplet.parseInt(round(PApplet.parseFloat(nPointsPerUpdate)*1000.f/get_fs_Hz_safe())/playback_speed_fac);
      if (nextPlayback_millis < 0) nextPlayback_millis = current_millis;
      nextPlayback_millis += increment_millis;

      // generate or read the data
      lastReadDataPacketInd = 0;
      for (int i = 0; i < nPointsPerUpdate; i++) {
        // println();
        dataPacketBuff[lastReadDataPacketInd].sampleIndex++;
        switch (eegDataSource) {
        case DATASOURCE_SYNTHETIC: //use synthetic data (for GUI debugging)
          synthesizeData(nchan, get_fs_Hz_safe(), openBCI.get_scale_fac_uVolts_per_count(), dataPacketBuff[lastReadDataPacketInd]);
          break;
        case DATASOURCE_PLAYBACKFILE:
          currentTableRowIndex=getPlaybackDataFromTable(playbackData_table, currentTableRowIndex, openBCI.get_scale_fac_uVolts_per_count(), openBCI.get_scale_fac_accel_G_per_count(), dataPacketBuff[lastReadDataPacketInd]);
          break;
        default:
          //no action
        }
        //gather the data into the "little buffer"
        for (int Ichan=0; Ichan < nchan; Ichan++) {
          //scale the data into engineering units..."microvolts"
          yLittleBuff_uV[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan]* openBCI.get_scale_fac_uVolts_per_count();
        }

        pointCounter++;
      } //close the loop over data points
      //if (eegDataSource==DATASOURCE_PLAYBACKFILE) println(appName + ": getDataIfAvailable: currentTableRowIndex = " + currentTableRowIndex);
      //println(appName + ": getDataIfAvailable: pointCounter = " + pointCounter);
    } // close "has enough time passed"
  }
  return pointCounter;
}

RunningMean avgBitRate = new RunningMean(10);  //10 point running average...at 5 points per second, this should be 2 second running average

public void processNewData() {

  //compute instantaneous byte rate
  float inst_byteRate_perSec = (int)(1000.f * ((float)(openBCI_byteCount - prevBytes)) / ((float)(millis() - prevMillis)));

  prevMillis=millis();           //store for next time
  prevBytes = openBCI_byteCount; //store for next time

  //compute smoothed byte rate
  avgBitRate.addValue(inst_byteRate_perSec);
  byteRate_perSec = (int)avgBitRate.calcMean();

  ////prepare to update the data buffers
  //float foo_val;

  //update the data buffers
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    //append the new data to the larger data buffer...because we want the plotting routines
    //to show more than just the most recent chunk of data.  This will be our "raw" data.
    appendAndShift(dataBuffY_uV[Ichan], yLittleBuff_uV[Ichan]);

    //make a copy of the data that we'll apply processing to.  This will be what is displayed on the full montage
    dataBuffY_filtY_uV[Ichan] = dataBuffY_uV[Ichan].clone();
  }

  //if you want to, re-reference the montage to make it be a mean-head reference
  if (false) rereferenceTheMontage(dataBuffY_filtY_uV);

  //apply additional processing for the time-domain montage plot (ie, filtering)
  dataProcessing.process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff);

  //apply user processing
  // ...yLittleBuff_uV[Ichan] is the most recent raw data since the last call to this processing routine
  // ...dataBuffY_filtY_uV[Ichan] is the full set of filtered data as shown in the time-domain plot in the GUI
  // ...fftBuff[Ichan] is the FFT data structure holding the frequency spectrum as shown in the freq-domain plot in the GUI
  // w_emg.process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff); //%%%
  // w_openbionics.process();
  
  dataProcessing_user.process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff);

  //look to see if the latest data is railed so that we can notify the user on the GUI
  for (int Ichan=0; Ichan < nchan; Ichan++) is_railed[Ichan].update(dataPacketBuff[lastReadDataPacketInd].values[Ichan]);

  //compute the electrode impedance. Do it in a very simple way [rms to amplitude, then uVolt to Volt, then Volt/Amp to Ohm]
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    // Calculate the impedance
    float impedance = (sqrt(2.0f)*dataProcessing.data_std_uV[Ichan]*1.0e-6f) / openBCI.get_leadOffDrive_amps();
    // Subtract the 2.2kOhm resistor
    impedance -= openBCI.get_series_resistor();
    // Verify the impedance is not less than 0
    if (impedance < 0) {
      // Incase impedance some how dipped below 2.2kOhm
      impedance = 0;
    }
    // Store to the global variable
    data_elec_imp_ohm[Ichan] = impedance;
  }
}

//helper function in handling the EEG data
public void appendAndShift(float[] data, float[] newData) {
  int nshift = newData.length;
  int end = data.length-nshift;
  for (int i=0; i < end; i++) {
    data[i]=data[i+nshift];  //shift data points down by 1
  }
  for (int i=0; i<nshift; i++) {
    data[end+i] = newData[i];  //append new data
  }
}

//help append and shift a single data
public void appendAndShift(float[] data, float newData) {
  int nshift = 1;
  int end = data.length-nshift;
  for (int i=0; i < end; i++) {
    data[i]=data[i+nshift];  //shift data points down by 1
  }
  data[end] = newData;  //append new data
}

final float sine_freq_Hz = 10.0f;
float[] sine_phase_rad = new float[nchan];

public void synthesizeData(int nchan, float fs_Hz, float scale_fac_uVolts_per_count, DataPacket_ADS1299 curDataPacket) {
  float val_uV;
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    if (isChannelActive(Ichan)) {
      val_uV = randomGaussian()*sqrt(fs_Hz/2.0f); // ensures that it has amplitude of one unit per sqrt(Hz) of signal bandwidth
      //val_uV = random(1)*sqrt(fs_Hz/2.0f); // ensures that it has amplitude of one unit per sqrt(Hz) of signal bandwidth
      if (Ichan==0) val_uV*= 10f;  //scale one channel higher

      if (Ichan==1) {
        //add sine wave at 10 Hz at 10 uVrms
        sine_phase_rad[Ichan] += 2.0f*PI * sine_freq_Hz / fs_Hz;
        if (sine_phase_rad[Ichan] > 2.0f*PI) sine_phase_rad[Ichan] -= 2.0f*PI;
        val_uV += 10.0f * sqrt(2.0f)*sin(sine_phase_rad[Ichan]);
      } else if (Ichan==2) {
        //50 Hz interference at 50 uVrms
        sine_phase_rad[Ichan] += 2.0f*PI * 50.0f / fs_Hz;  //60 Hz
        if (sine_phase_rad[Ichan] > 2.0f*PI) sine_phase_rad[Ichan] -= 2.0f*PI;
        val_uV += 50.0f * sqrt(2.0f)*sin(sine_phase_rad[Ichan]);    //20 uVrms
      } else if (Ichan==3) {
        //60 Hz interference at 50 uVrms
        sine_phase_rad[Ichan] += 2.0f*PI * 60.0f / fs_Hz;  //50 Hz
        if (sine_phase_rad[Ichan] > 2.0f*PI) sine_phase_rad[Ichan] -= 2.0f*PI;
        val_uV += 50.0f * sqrt(2.0f)*sin(sine_phase_rad[Ichan]);  //20 uVrms
      }
    } else {
      val_uV = 0.0f;
    }
    curDataPacket.values[Ichan] = (int) (0.5f+ val_uV / scale_fac_uVolts_per_count); //convert to counts, the 0.5 is to ensure rounding
  }
}

//some data initialization routines
public void prepareData(float[] dataBuffX, float[][] dataBuffY_uV, float fs_Hz) {
  //initialize the x and y data
  int xoffset = dataBuffX.length - 1;
  for (int i=0; i < dataBuffX.length; i++) {
    dataBuffX[i] = ((float)(i-xoffset)) / fs_Hz; //x data goes from minus time up to zero
    for (int Ichan = 0; Ichan < nchan; Ichan++) {
      dataBuffY_uV[Ichan][i] = 0f;  //make the y data all zeros
    }
  }
}


public void initializeFFTObjects(FFT[] fftBuff, float[][] dataBuffY_uV, int N, float fs_Hz) {

  float[] fooData;
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    //make the FFT objects...Following "SoundSpectrum" example that came with the Minim library
    //fftBuff[Ichan] = new FFT(Nfft, fs_Hz);  //I can't have this here...it must be in setup
    fftBuff[Ichan].window(FFT.HAMMING);

    //do the FFT on the initial data
    if (isFFTFiltered == true) {
      fooData = dataBuffY_filtY_uV[Ichan];  //use the filtered data for the FFT
    } else {
      fooData = dataBuffY_uV[Ichan];  //use the raw data for the FFT
    }
    fooData = Arrays.copyOfRange(fooData, fooData.length-Nfft, fooData.length);
    fftBuff[Ichan].forward(fooData); //compute FFT on this channel of data
  }
}


public int getPlaybackDataFromTable(Table datatable, int currentTableRowIndex, float scale_fac_uVolts_per_count, float scale_fac_accel_G_per_count, DataPacket_ADS1299 curDataPacket) {
  float val_uV = 0.0f;
  float[] acc_G = new float[n_aux_ifEnabled];
  boolean acc_newData = false;

  //check to see if we can load a value from the table
  if (currentTableRowIndex >= datatable.getRowCount()) {
    //end of file
    println(appName + ": getPlaybackDataFromTable: hit the end of the playback data file.  starting over...");
    hasRepeated = true;
    //if (isRunning) stopRunning();
    currentTableRowIndex = 0;
  } else {
    //get the row
    TableRow row = datatable.getRow(currentTableRowIndex);
    currentTableRowIndex++; //increment to the next row

    //get each value
    for (int Ichan=0; Ichan < nchan; Ichan++) {
      if (isChannelActive(Ichan) && (Ichan < datatable.getColumnCount())) {
        val_uV = row.getFloat(Ichan);
      } else {
        //use zeros for the missing channels
        val_uV = 0.0f;
      }

      //put into data structure
      curDataPacket.values[Ichan] = (int) (0.5f+ val_uV / scale_fac_uVolts_per_count); //convert to counts, the 0.5 is to ensure rounding
    }

   // get accelerometer data
   try{
     for (int Iacc=0; Iacc < n_aux_ifEnabled; Iacc++) {
        if (Iacc < datatable.getColumnCount()) {
          acc_G[Iacc] = row.getFloat(Iacc + nchan);
        } else {
          //use zeros for bad data :)
          acc_G[Iacc] = 0.0f;
        }

        //put into data structure
        curDataPacket.auxValues[Iacc] = (int) (0.5f+ acc_G[Iacc] / scale_fac_accel_G_per_count); //convert to counts, the 0.5 is to ensure rounding

        // Wangshu Dec.6 2016
        // as long as xyz are not zero at the same time, it should be fine...otherwise it will ignore it.
        if (acc_G[Iacc]!= 0) {
          acc_newData = true;
        }
      }
   } catch (ArrayIndexOutOfBoundsException e){
      // println("Data does not exist... possibly an old file.");
   }


    if (acc_newData) {
      for (int Iacc=0; Iacc < n_aux_ifEnabled; Iacc++) {
        appendAndShift(accelerometerBuff[Iacc], acc_G[Iacc]);
      }
    }

    // get time stamp
    if (!isOldData) curTimestamp = row.getString(nchan+3);

    //int localnchan = nchan;

    if(!isRunning){
      try{
        if(!isOldData) row.getString(nchan+4);
        else row.getString(nchan+3);

        nchan = 16;
      }
      catch (ArrayIndexOutOfBoundsException e){ println("8 Channel");}
    }

  }
  return currentTableRowIndex;
}

//------------------------------------------------------------------------
//                          CLASSES
//------------------------------------------------------------------------

class DataProcessing {
  private float fs_Hz;  //sample rate
  private int nchan;
  final int N_FILT_CONFIGS = 5;
  FilterConstants[] filtCoeff_bp = new FilterConstants[N_FILT_CONFIGS];
  final int N_NOTCH_CONFIGS = 3;
  FilterConstants[] filtCoeff_notch = new FilterConstants[N_NOTCH_CONFIGS];
  private int currentFilt_ind = 3;
  private int currentNotch_ind = 0;  // set to 0 to default to 60Hz, set to 1 to default to 50Hz
  float data_std_uV[];
  float polarity[];


  DataProcessing(int NCHAN, float sample_rate_Hz) {
    nchan = NCHAN;
    fs_Hz = sample_rate_Hz;
    data_std_uV = new float[nchan];
    polarity = new float[nchan];


    //check to make sure the sample rate is acceptable and then define the filters
    if (abs(fs_Hz-250.0f) < 1.0f) {
      defineFilters(0);
    } else if (abs(fs_Hz-200.0f) < 1.0f) {
      defineFilters(1);
    } else {
      println("EEG_Processing: *** ERROR *** Filters can currently only work at 250 Hz or 200 Hz");
      defineFilters(0);  //define the filters anyway just so that the code doesn't bomb
    }
  }

  public float getSampleRateHz() {
    return fs_Hz;
  };

  //define filters...assumes sample rate of 250 Hz !!!!!
  private void defineFilters(int _mode) {
    int mode = _mode; // 0 means classic OpenBCI board, 1 means ganglion
    int n_filt;
    double[] b, a, b2, a2;
    String filt_txt, filt_txt2;
    String short_txt, short_txt2;

    switch(mode) {
      // classic OpenBCI board, sampling rate 250 Hz
    case 0:
      //loop over all of the pre-defined filter types
      n_filt = filtCoeff_notch.length;
      for (int Ifilt=0; Ifilt < n_filt; Ifilt++) {
        switch (Ifilt) {
        case 0:
          //60 Hz notch filter, assumed fs = 250 Hz.  2nd Order Butterworth: b, a = signal.butter(2,[59.0 61.0]/(fs_Hz / 2.0), 'bandstop')
          b2 = new double[] { 9.650809863447347e-001f, -2.424683201757643e-001f, 1.945391494128786e+000f, -2.424683201757643e-001f, 9.650809863447347e-001f };
          a2 = new double[] { 1.000000000000000e+000f, -2.467782611297853e-001f, 1.944171784691352e+000f, -2.381583792217435e-001f, 9.313816821269039e-001f  };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 60Hz", "60Hz");
          break;
        case 1:
          //50 Hz notch filter, assumed fs = 250 Hz.  2nd Order Butterworth: b, a = signal.butter(2,[49.0 51.0]/(fs_Hz / 2.0), 'bandstop')
          b2 = new double[] { 0.96508099f, -1.19328255f, 2.29902305f, -1.19328255f, 0.96508099f };
          a2 = new double[] { 1.0f, -1.21449348f, 2.29780334f, -1.17207163f, 0.93138168f };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 50Hz", "50Hz");
          break;
        case 2:
          //no notch filter
          b2 = new double[] { 1.0f };
          a2 = new double[] { 1.0f };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "No Notch", "None");
          break;
        }
      } // end loop over notch filters

      n_filt = filtCoeff_bp.length;
      for (int Ifilt=0; Ifilt<n_filt; Ifilt++) {
        //define bandpass filter
        switch (Ifilt) {
        case 0:
          //butter(2,[1 50]/(250/2));  %bandpass filter
          b = new double[] {
            2.001387256580675e-001f, 0.0f, -4.002774513161350e-001f, 0.0f, 2.001387256580675e-001f
          };
          a = new double[] {
            1.0f, -2.355934631131582e+000f, 1.941257088655214e+000f, -7.847063755334187e-001f, 1.999076052968340e-001f
          };
          filt_txt = "Bandpass 1-50Hz";
          short_txt = "1-50 Hz";
          break;
        case 1:
          //butter(2,[7 13]/(250/2));
          b = new double[] {
            5.129268366104263e-003f, 0.0f, -1.025853673220853e-002f, 0.0f, 5.129268366104263e-003f
          };
          a = new double[] {
            1.0f, -3.678895469764040e+000f, 5.179700413522124e+000f, -3.305801890016702e+000f, 8.079495914209149e-001f
          };
          filt_txt = "Bandpass 7-13Hz";
          short_txt = "7-13 Hz";
          break;
        case 2:
          //[b,a]=butter(2,[15 50]/(250/2)); %matlab command
          b = new double[] {
            1.173510367246093e-001f, 0.0f, -2.347020734492186e-001f, 0.0f, 1.173510367246093e-001f
          };
          a = new double[] {
            1.0f, -2.137430180172061e+000f, 2.038578008108517e+000f, -1.070144399200925e+000f, 2.946365275879138e-001f
          };
          filt_txt = "Bandpass 15-50Hz";
          short_txt = "15-50 Hz";
          break;
        case 3:
          //[b,a]=butter(2,[5 50]/(250/2)); %matlab command
          b = new double[] {
            1.750876436721012e-001f, 0.0f, -3.501752873442023e-001f, 0.0f, 1.750876436721012e-001f
          };
          a = new double[] {
            1.0f, -2.299055356038497e+000f, 1.967497759984450e+000f, -8.748055564494800e-001f, 2.196539839136946e-001f
          };
          filt_txt = "Bandpass 5-50Hz";
          short_txt = "5-50 Hz";
          break;
        default:
          //no filtering
          b = new double[] {
            1.0f
          };
          a = new double[] {
            1.0f
          };
          filt_txt = "No BP Filter";
          short_txt = "No Filter";
        }  //end switch block

        //create the bandpass filter
        filtCoeff_bp[Ifilt] =  new FilterConstants(b, a, filt_txt, short_txt);
      } //end loop over band pass filters

      break;

      // Ganglion board, sampling rate 200 Hz
    case 1:
      //loop over all of the pre-defined filter types
      n_filt = filtCoeff_notch.length;
      for (int Ifilt=0; Ifilt < n_filt; Ifilt++) {
        switch (Ifilt) {
        case 0:
          //60 Hz notch filter, assumed fs = 200 Hz.  2nd Order Butterworth: b, a = signal.butter(2,[59.0 61.0]/(fs_Hz / 2.0), 'bandstop')
          b2 = new double[] { 0.956543225556876f, 1.18293615779028f, 2.27881429174347f, 1.18293615779028f, 0.956543225556876f };
          a2 = new double[] { 1, 1.20922304075909f, 2.27692490805579f, 1.15664927482146f, 0.914975834801432f };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 60Hz", "60Hz");
          break;
        case 1:
          //50 Hz notch filter, assumed fs = 200 Hz.  2nd Order Butterworth: b, a = signal.butter(2,[49.0 51.0]/(fs_Hz / 2.0), 'bandstop')
          b2 = new double[] { 0.956543225556877f, -2.34285519884863e-16f, 1.91308645111375f, -2.34285519884863e-16f, 0.956543225556877f};
          a2 = new double[] { 1, -1.02695629777827e-15f, 1.91119706742607f, -1.01654795692241e-15f, 0.914975834801435f};
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 50Hz", "50Hz");
          break;
        case 2:
          //no notch filter
          b2 = new double[] { 1.0f };
          a2 = new double[] { 1.0f };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "No Notch", "None");
          break;
        }
      } // end loop over notch filters

      n_filt = filtCoeff_bp.length;
      for (int Ifilt=0; Ifilt<n_filt; Ifilt++) {
        //define bandpass filter
        switch (Ifilt) {
        case 0:
          //butter(2,[1 50]/(200/2));  %bandpass filter
          b = new double[] {
            0.283751216219318f, 0, -0.567502432438636f, 0, 0.283751216219318f
          };
          a = new double[] {
            1, -1.97380379923172f, 1.17181238127012f, -0.368664525962831f, 0.171812381270120f
          };
          filt_txt = "Bandpass 1-50Hz";
          short_txt = "1-50 Hz";
          break;
        case 1:
          //butter(2,[7 13]/(200/2));
          b = new double[] {
            0.00782020803349883f, 0, -0.0156404160669977f, 0, 0.00782020803349883f
          };
          a = new double[] {
            1, -3.56776916484310f, 4.92946172209398f, -3.12070317627516f, 0.766006600943266f
          };
          filt_txt = "Bandpass 7-13Hz";
          short_txt = "7-13 Hz";
          break;
        case 2:
          //[b,a]=butter(2,[15 50]/(200/2)); %matlab command
          b = new double[] {
            0.167483800127017f, 0, -0.334967600254034f, 0, 0.167483800127017f
          };
          a = new double[] {
            1, -1.56695061045088f, 1.22696619781982f, -0.619519163981230f, 0.226966197819818f
          };
          filt_txt = "Bandpass 15-50Hz";
          short_txt = "15-50 Hz";
          break;
        case 3:
          //[b,a]=butter(2,[5 50]/(200/2)); %matlab command
          b = new double[] {
            0.248341078962540f, 0, -0.496682157925080f, 0, 0.248341078962540f
          };
          a = new double[] {
            1, -1.86549482213123f, 1.17757811892770f, -0.460665534278457f, 0.177578118927698f
          };
          filt_txt = "Bandpass 5-50Hz";
          short_txt = "5-50 Hz";
          break;
        default:
          //no filtering
          b = new double[] {
            1.0f
          };
          a = new double[] {
            1.0f
          };
          filt_txt = "No BP Filter";
          short_txt = "No Filter";
        }  //end switch block

        //create the bandpass filter
        filtCoeff_bp[Ifilt] =  new FilterConstants(b, a, filt_txt, short_txt);
      } //end loop over band pass filters

      break;
    }
  } //end defineFilters method

  public String getFilterDescription() {
    return filtCoeff_bp[currentFilt_ind].name + ", " + filtCoeff_notch[currentNotch_ind].name;
  }
  public String getShortFilterDescription() {
    return filtCoeff_bp[currentFilt_ind].short_name;
  }
  public String getShortNotchDescription() {
    return filtCoeff_notch[currentNotch_ind].short_name;
  }

  public void incrementFilterConfiguration() {
    //increment the index
    currentFilt_ind++;
    if (currentFilt_ind >= N_FILT_CONFIGS) currentFilt_ind = 0;
  }

  public void incrementNotchConfiguration() {
    //increment the index
    currentNotch_ind++;
    if (currentNotch_ind >= N_NOTCH_CONFIGS) currentNotch_ind = 0;
  }

  public void process(float[][] data_newest_uV, //holds raw EEG data that is new since the last call
    float[][] data_long_uV, //holds a longer piece of buffered EEG data, of same length as will be plotted on the screen
    float[][] data_forDisplay_uV, //put data here that should be plotted on the screen
    FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data

    //loop over each EEG channel
    for (int Ichan=0; Ichan < nchan; Ichan++) {

      //filter the data in the time domain
      filterIIR(filtCoeff_notch[currentNotch_ind].b, filtCoeff_notch[currentNotch_ind].a, data_forDisplay_uV[Ichan]); //notch
      filterIIR(filtCoeff_bp[currentFilt_ind].b, filtCoeff_bp[currentFilt_ind].a, data_forDisplay_uV[Ichan]); //bandpass

      //compute the standard deviation of the filtered signal...this is for the head plot
      float[] fooData_filt = dataBuffY_filtY_uV[Ichan];  //use the filtered data
      fooData_filt = Arrays.copyOfRange(fooData_filt, fooData_filt.length-((int)fs_Hz), fooData_filt.length);   //just grab the most recent second of data
      data_std_uV[Ichan]=std(fooData_filt); //compute the standard deviation for the whole array "fooData_filt"
    } //close loop over channels


    // calculate FFT after filter

    //println("PPP" + fftBuff[0].specSize());
    float prevFFTdata[] = new float[fftBuff[0].specSize()];
    double foo;

    //update the FFT (frequency spectrum)
    // println("nchan = " + nchan);
    for (int Ichan=0; Ichan < nchan; Ichan++) {

      //copy the previous FFT data...enables us to apply some smoothing to the FFT data
      for (int I=0; I < fftBuff[Ichan].specSize(); I++) {
        prevFFTdata[I] = fftBuff[Ichan].getBand(I); //copy the old spectrum values
      }

      //prepare the data for the new FFT
      float[] fooData;
      if (isFFTFiltered == true) {
        fooData = dataBuffY_filtY_uV[Ichan];  //use the filtered data for the FFT
      } else {
        fooData = dataBuffY_uV[Ichan];  //use the raw data for the FFT
      }
      fooData = Arrays.copyOfRange(fooData, fooData.length-Nfft, fooData.length);   //trim to grab just the most recent block of data
      float meanData = mean(fooData);  //compute the mean
      for (int I=0; I < fooData.length; I++) fooData[I] -= meanData; //remove the mean (for a better looking FFT

      //compute the FFT
      fftBuff[Ichan].forward(fooData); //compute FFT on this channel of data

      //convert to uV_per_bin...still need to confirm the accuracy of this code.
      //Do we need to account for the power lost in the windowing function?   CHIP  2014-10-24
      for (int I=0; I < fftBuff[Ichan].specSize(); I++) {  //loop over each FFT bin
        fftBuff[Ichan].setBand(I, (float)(fftBuff[Ichan].getBand(I) / fftBuff[Ichan].specSize()));
      }

      //average the FFT with previous FFT data so that it makes it smoother in time
      double min_val = 0.01d;
      for (int I=0; I < fftBuff[Ichan].specSize(); I++) {   //loop over each fft bin
        if (prevFFTdata[I] < min_val) prevFFTdata[I] = (float)min_val; //make sure we're not too small for the log calls
        foo = fftBuff[Ichan].getBand(I);
        if (foo < min_val) foo = min_val; //make sure this value isn't too small

        if (true) {
          //smooth in dB power space
          foo =   (1.0d-smoothFac[smoothFac_ind]) * java.lang.Math.log(java.lang.Math.pow(foo, 2));
          foo += smoothFac[smoothFac_ind] * java.lang.Math.log(java.lang.Math.pow((double)prevFFTdata[I], 2));
          foo = java.lang.Math.sqrt(java.lang.Math.exp(foo)); //average in dB space
        } else {
          //smooth (average) in linear power space
          foo =   (1.0d-smoothFac[smoothFac_ind]) * java.lang.Math.pow(foo, 2);
          foo+= smoothFac[smoothFac_ind] * java.lang.Math.pow((double)prevFFTdata[I], 2);
          // take sqrt to be back into uV_rtHz
          foo = java.lang.Math.sqrt(foo);
        }
        fftBuff[Ichan].setBand(I, (float)foo); //put the smoothed data back into the fftBuff data holder for use by everyone else
      } //end loop over FFT bins
    } //end the loop over channels.


    //find strongest channel
    int refChanInd = findMax(data_std_uV);
    //println("EEG_Processing: strongest chan (one referenced) = " + (refChanInd+1));
    float[] refData_uV = dataBuffY_filtY_uV[refChanInd];  //use the filtered data
    refData_uV = Arrays.copyOfRange(refData_uV, refData_uV.length-((int)fs_Hz), refData_uV.length);   //just grab the most recent second of data


    //compute polarity of each channel
    for (int Ichan=0; Ichan < nchan; Ichan++) {
      float[] fooData_filt = dataBuffY_filtY_uV[Ichan];  //use the filtered data
      fooData_filt = Arrays.copyOfRange(fooData_filt, fooData_filt.length-((int)fs_Hz), fooData_filt.length);   //just grab the most recent second of data
      float dotProd = calcDotProduct(fooData_filt, refData_uV);
      if (dotProd >= 0.0f) {
        polarity[Ichan]=1.0f;
      } else {
        polarity[Ichan]=-1.0f;
      }
    }
  }
}

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

DataProcessing_User dataProcessing_user;
boolean drawEMG = false; //if true... toggles on EEG_Processing_User.draw and toggles off the headplot in Gui_Manager
boolean drawAccel = false;
boolean drawPulse = false;
boolean drawFFT = true;
boolean drawBionics = false;
boolean drawHead = true;


String oldCommand = "";
boolean hasGestured = false;

//------------------------------------------------------------------------
//                            Classes
//------------------------------------------------------------------------

class DataProcessing_User {
  private float fs_Hz;  //sample rate
  private int n_chan;

  boolean switchesActive = false;


  Button leftConfig = new Button(3*(width/4) - 65,height/4 - 120,20,20,"\\/",fontInfo.buttonLabel_size);
  Button midConfig = new Button(3*(width/4) + 63,height/4 - 120,20,20,"\\/",fontInfo.buttonLabel_size);
  Button rightConfig = new Button(3*(width/4) + 190,height/4 - 120,20,20,"\\/",fontInfo.buttonLabel_size);



  //class constructor
  DataProcessing_User(int NCHAN, float sample_rate_Hz) {
    n_chan = NCHAN;
    fs_Hz = sample_rate_Hz;
  }

  //add some functions here...if you'd like

  //here is the processing routine called by the OpenBCI main program...update this with whatever you'd like to do
  public void process(float[][] data_newest_uV, //holds raw bio data that is new since the last call
    float[][] data_long_uV, //holds a longer piece of buffered EEG data, of same length as will be plotted on the screen
    float[][] data_forDisplay_uV, //this data has been filtered and is ready for plotting on the screen
    FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data

    //for example, you could loop over each EEG channel to do some sort of time-domain processing
    //using the sample values that have already been filtered, as will be plotted on the display
    float EEG_value_uV;




    }

  }

//////////////////////////////////////
//
// This file contains classes that are helpful for debugging, as well as the HelpWidget,
// which is used to give feedback to the GUI user in the small text window at the bottom of the GUI
//
// Created: Conor Russomanno, June 2016
// Based on code: Chip Audette, Oct 2013 - Dec 2014
//
//
/////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

//set true if you want more verbosity in console.. verbosePrint("print_this_thing") is used to output feedback when isVerbose = true
boolean isVerbose = true;

//Help Widget initiation
HelpWidget helpWidget;

//use signPost(String identifier) to print 'identifier' text and time since last signPost() for debugging latency/timing issues
boolean printSignPosts = true;
float millisOfLastSignPost = 0.0f;
float millisSinceLastSignPost = 0.0f;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

public void verbosePrint(String _string) {
  if (isVerbose) {
    println(_string);
  }
}

public void delay(int delay)
{
  int time = millis();
  while (millis() - time <= delay);
}

//this class is used to create the help widget that provides system feedback in response to interactivity
//it is intended to serve as a pseudo-console, allowing us to print useful information to the interface as opposed to an IDE console

class HelpWidget {

  public float x, y, w, h;
  // ArrayList<String> prevOutputs; //growing list of all previous system interactivity

  String currentOutput = "..."; //current text shown in help widget, based on most recent command

  int padding = 5;

  HelpWidget(float _xPos, float _yPos, float _width, float _height) {
    x = _xPos;
    y = _yPos;
    w = _width;
    h = _height;
  }

  public void update() {
    //nothing needed here
  }

  public void draw() {

    pushStyle();

    if(colorScheme == COLOR_SCHEME_DEFAULT){
      // draw background of widget
      stroke(bgColor);
      fill(255);
      rect(-1, height-h, width+2, h);
      noStroke();

      //draw bg of text field of widget
      strokeWeight(1);
      stroke(color(0, 5, 11));
      fill(color(0, 5, 11));
      rect(x + padding, height-h + padding, width - padding*2, h - padding *2);

      textFont(p4);
      textSize(14);
      fill(255);
      textAlign(LEFT, TOP);
      text(currentOutput, padding*2, height - h + padding);
    } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
      // draw background of widget
      stroke(bgColor);
      fill(31,69,110);
      rect(-1, height-h, width+2, h);
      noStroke();

      //draw bg of text field of widget
      strokeWeight(1);
      stroke(color(0, 5, 11));
      fill(200);
      fill(255);
      // fill(57,128,204);
      rect(x + padding, height-h + padding, width - padding*2, h - padding *2);

      textFont(p4);
      textSize(14);
      fill(bgColor);
      // fill(57,128,204);
      // fill(openbciBlue);
      textAlign(LEFT, TOP);
      text(currentOutput, padding*2, height - h + padding);
    }
    
    popStyle();
  }

  public void output(String _output) {
    currentOutput = _output;
    // prevOutputs.add(_output);
  }
};

public void output(String _output) {
  helpWidget.output(_output);
}

// created 2/10/16 by Conor Russomanno to dissect the aspects of the GUI that are slowing it down
// here I will create methods used to identify where there are inefficiencies in the code
// note to self: make sure to check the frameRate() in setup... switched from 16 to 30... working much faster now... still a useful method below.
// --------------------------------------------------------------  START -------------------------------------------------------------------------------

//method for printing out an ["indentifier"][millisSinceLastSignPost] for debugging purposes... allows us to look at what is taking too long.
public void signPost(String identifier) {
  if (printSignPosts) {
    millisSinceLastSignPost = millis() - millisOfLastSignPost;
    println("SIGN POST: [" + identifier + "][" + millisSinceLastSignPost + "]");
    millisOfLastSignPost = millis();
  }
}
// ---------------------------------------------------------------- FINISH -----------------------------------------------------------------------------
// /////////////////////////////////////////////////////////////////////////////////
// //
// //  Emg_Widget is used to visiualze EMG data by channel, and to trip events
// //
// //  Created: Colin Fausnaught, August 2016 (with a lot of reworked code from Tao)
// //
// //  Custom widget to visiualze EMG data. Features dragable thresholds, serial
// //  out communication, channel configuration, digital and analog events.
// //
// //  KNOWN ISSUES: Cannot resize with window dragging events
// //
// //  TODO: Add dynamic threshold functionality
// ////////////////////////////////////////////////////////////////////////////////
//
//
// //------------------------------------------------------------------------
// //                       Global Variables & Instances
// //------------------------------------------------------------------------
//
// Button configButton;
// //Serial serialOutEMG;
// ControlP5 cp5Serial;
// String serialNameEMG;
// String baudEMG;

//
//
// //------------------------------------------------------------------------
// //                            Classes
// //------------------------------------------------------------------------
//
// class EMG_Widget extends Container {
//
//   private float fs_Hz; //sample rate
//   private int nchan;
//   private int lastChan = 0;
//   PApplet parent;
//   String oldCommand = "";
//   int parentContainer = 3;
//   PFont f = createFont("Arial Bold", 24); //for "FFT Plot" Widget Title
//
//   Motor_Widget[] motorWidgets;
//   TripSlider[] tripSliders;
//   TripSlider[] untripSliders;
//
//   public Config_Widget configWidget;
//
//   class Motor_Widget {
//     //variables
//     boolean isTriggered = false;
//     float upperThreshold = 25;        //default uV upper threshold value ... this will automatically change over time
//     float lowerThreshold = 0;         //default uV lower threshold value ... this will automatically change over time
//     int averagePeriod = 250;          //number of data packets to average over (250 = 1 sec)
//     int thresholdPeriod = 1250;       //number of packets
//     int ourChan = 0;                  //channel being monitored ... "3 - 1" means channel 3 (with a 0 index)
//     float myAverage = 0.0;            //this will change over time ... used for calculations below
//     float acceptableLimitUV = 200;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
//     //prez related
//     boolean switchTripped = false;
//     int switchCounter = 0;
//     float timeOfLastTrip = 0;
//     float tripThreshold = 0.75;
//     float untripThreshold = 0.5;
//     //if writing to a serial port
//     int output = 0;                   //value between 0-255 that is the relative position of the current uV average between the rolling lower and upper uV thresholds
//     float output_normalized = 0;      //converted to between 0-1
//     float output_adjusted = 0;        //adjusted depending on range that is expected on the other end, ie 0-255?
//     boolean analogBool = true;        //Analog events?
//     boolean digitalBool = true;       //Digital events?
//   }
//   //Constructor
//   EMG_Widget(int NCHAN, float sample_rate_Hz, Container c, PApplet p) {
//     super(c, "WHOLE");
//     x = (int)container[parentContainer].x;
//     y = (int)container[parentContainer].y;
//     h = (int)container[parentContainer].h;
//     w = (int)container[parentContainer].w;
//
//     parent = p;
//     cp5Serial = new ControlP5(p);
//
//     this.nchan = NCHAN;
//     this.fs_Hz = sample_rate_Hz;
//     tripSliders = new TripSlider[NCHAN];
//     untripSliders = new TripSlider[NCHAN];
//     motorWidgets = new Motor_Widget[NCHAN];
//
//     for (int i = 0; i < NCHAN; i++) {
//       motorWidgets[i] = new Motor_Widget();
//       motorWidgets[i].ourChan = i;
//     }
//
//     initSliders(w, h);
//
//     configButton = new Button(int(x), int(y + h/14), 20, 20, "O", fontInfo.buttonLabel_size);
//     configWidget = new Config_Widget(NCHAN, sample_rate_Hz, c, motorWidgets);
//   }
//
//
//   //Initalizes the threshold sliders
//   void initSliders(float rw, float rh) {
//     //Stole some logic from the rectangle drawing in draw()
//     int rowNum = 4;
//     int colNum = motorWidgets.length / rowNum;
//     int index = 0;
//
//     float rowOffset = rh / rowNum;
//     float colOffset = rw / colNum;
//
//     if (nchan == 4) {
//       for (int i = 0; i < rowNum; i++) {
//         for (int j = 0; j < colNum; j++) {
//
//           if (i > 2) {
//             tripSliders[index] = new TripSlider(int(752 + (j * 205)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, true, motorWidgets[index]);
//             untripSliders[index] = new TripSlider(int(752 + (j * 205)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, false, motorWidgets[index]);
//           } else {
//             tripSliders[index] = new TripSlider(int(752 + (j * 205)), int(117 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, true, motorWidgets[index]);
//             untripSliders[index] = new TripSlider(int(752 + (j * 205)), int(117 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, false, motorWidgets[index]);
//           }
//
//           tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
//           untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
//           index++;
//         }
//       }
//     } else if (nchan == 8) {
//       for (int i = 0; i < rowNum; i++) {
//         for (int j = 0; j < colNum; j++) {
//
//           tripSliders[index] = new TripSlider(int(5*colOffset/8), int(2 * rowOffset / 8), 0, int((3*colOffset/32)), 2, tripSliders, true, motorWidgets[index]);
//           untripSliders[index] = new TripSlider(int(5*colOffset/8), int(2 * rowOffset / 8), 0, int(3*colOffset/32), 2, tripSliders, false, motorWidgets[index]);
//
//           tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
//           untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
//           index++;
//         }
//       }
//     } else if (nchan == 16) {
//       for (int i = 0; i < rowNum; i++) {
//         for (int j = 0; j < colNum; j++) {
//
//
//           if ( j < 2) {
//             //tripSliders[index] = new TripSlider(int(683 + (j * 103)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders,true, motorWidgets[index]);
//             //untripSliders[index] = new TripSlider(int(683 + (j * 103)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders,false, motorWidgets[index]);
//
//             tripSliders[index] = new TripSlider(int(683 + (j * 103)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, true, motorWidgets[index]);
//             untripSliders[index] = new TripSlider(int(683 + (j * 103)), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, false, motorWidgets[index]);
//           } else {
//             tripSliders[index] = new TripSlider(int(683 + (j * 103) - 1), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, true, motorWidgets[index]);
//             untripSliders[index] = new TripSlider(int(683 + (j * 103) - 1), int(118 + (i * 86)), 0, int(3*colOffset/32), 2, tripSliders, false, motorWidgets[index]);
//           }
//
//           tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
//           untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
//           index++;
//           println(index);
//         }
//       }
//     }
//   }
//
//   public void process(float[][] data_newest_uV, //holds raw EEG data that is new since the last call
//     float[][] data_long_uV, //holds a longer piece of buffered EEG data, of same length as will be plotted on the screen
//     float[][] data_forDisplay_uV, //this data has been filtered and is ready for plotting on the screen
//     FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data
//
//     //for example, you could loop over each EEG channel to do some sort of time-domain processing
//     //using the sample values that have already been filtered, as will be plotted on the display
//     //float EEG_value_uV;
//
//     //looping over channels and analyzing input data
//     for (Motor_Widget cfc : motorWidgets) {
//       cfc.myAverage = 0.0;
//       for (int i = data_forDisplay_uV[cfc.ourChan].length - cfc.averagePeriod; i < data_forDisplay_uV[cfc.ourChan].length; i++) {
//         if (abs(data_forDisplay_uV[cfc.ourChan][i]) <= cfc.acceptableLimitUV) { //prevent BIG spikes from effecting the average
//           cfc.myAverage += abs(data_forDisplay_uV[cfc.ourChan][i]);  //add value to average ... we will soon divide by # of packets
//         } else {
//           cfc.myAverage += cfc.acceptableLimitUV; //if it's greater than the limit, just add the limit
//         }
//       }
//       cfc.myAverage = cfc.myAverage / float(cfc.averagePeriod); //finishing the average
//
//       if (cfc.myAverage >= cfc.upperThreshold && cfc.myAverage <= cfc.acceptableLimitUV) { //
//         cfc.upperThreshold = cfc.myAverage;
//       }
//       if (cfc.myAverage <= cfc.lowerThreshold) {
//         cfc.lowerThreshold = cfc.myAverage;
//       }
//       if (cfc.upperThreshold >= (cfc.myAverage + 35)) {
//         cfc.upperThreshold *= .97;
//       }
//       if (cfc.lowerThreshold <= cfc.myAverage) {
//         cfc.lowerThreshold += (10 - cfc.lowerThreshold)/(frameRate * 5); //have lower threshold creep upwards to keep range tight
//       }
//       //output_L = (int)map(myAverage_L, lowerThreshold_L, upperThreshold_L, 0, 255);
//       cfc.output_normalized = map(cfc.myAverage, cfc.lowerThreshold, cfc.upperThreshold, 0, 1);
//       cfc.output_adjusted = ((-0.1/(cfc.output_normalized*255.0)) + 255.0);
//
//
//
//       //=============== TRIPPIN ==================
//       //= Just calls all the trip events         =
//       //==========================================
//
//       switch(cfc.ourChan) {
//
//         case 0:
//           if (configWidget.digital.wasPressed) digitalEventChan0(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan0(cfc);
//           break;
//         case 1:
//           if (configWidget.digital.wasPressed) digitalEventChan1(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan1(cfc);
//           break;
//         case 2:
//           if (configWidget.digital.wasPressed) digitalEventChan2(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan2(cfc);
//           break;
//         case 3:
//           if (configWidget.digital.wasPressed) digitalEventChan3(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan3(cfc);
//           break;
//         case 4:
//           if (configWidget.digital.wasPressed) digitalEventChan4(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan4(cfc);
//           break;
//         case 5:
//           if (configWidget.digital.wasPressed) digitalEventChan5(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan5(cfc);
//           break;
//         case 6:
//           if (configWidget.digital.wasPressed) digitalEventChan6(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan6(cfc);
//           break;
//         case 7:
//           if (configWidget.digital.wasPressed) digitalEventChan7(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan7(cfc);
//           break;
//         case 8:
//           if (configWidget.digital.wasPressed) digitalEventChan8(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan8(cfc);
//           break;
//         case 9:
//           if (configWidget.digital.wasPressed) digitalEventChan9(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan9(cfc);
//           break;
//         case 10:
//           if (configWidget.digital.wasPressed) digitalEventChan10(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan10(cfc);
//           break;
//         case 11:
//           if (configWidget.digital.wasPressed) digitalEventChan11(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan11(cfc);
//           break;
//         case 12:
//           if (configWidget.digital.wasPressed) digitalEventChan12(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan12(cfc);
//           break;
//         case 13:
//           if (configWidget.digital.wasPressed) digitalEventChan13(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan13(cfc);
//           break;
//         case 14:
//           if (configWidget.digital.wasPressed) digitalEventChan14(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan14(cfc);
//           break;
//         case 15:
//           if (configWidget.digital.wasPressed) digitalEventChan15(cfc);
//           if (configWidget.analog.wasPressed) analogEventChan15(cfc);
//           break;
//         default:
//           break;
//         }
//       }
//
//     //=================== OpenBionics switch example ==============================
//
//     //if (millis() - motorWidgets[1].timeOfLastTrip >= 2000 && serialOutEMG != null) {
//     //  switch(motorWidgets[1].switchCounter){
//     //    case 1:
//     //      switch(motorWidgets[0].switchCounter){
//     //        case 1:
//     //          //RED CIRCLE FOR JAW, RED FOR BROW
//     //          //hand.write(oldCommand);
//     //          break;
//     //        case 2:
//     //          //GREEN CIRCLE FOR JAW, RED FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "1234";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 3:
//     //          //BLUE CIRCLE FOR JAW, RED FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "01";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 4:
//     //          //VIOLET CIRCLE FOR JAW, RED FOR BROW
//     //          serialOutEMG.write("0");
//     //          break;
//     //      }
//     //      break;
//     //    case 2:
//     //      //println("Two Brow Raises");
//     //      switch(motorWidgets[0].switchCounter){
//     //        case 1:
//     //          //RED CIRCLE FOR JAW, GREEN FOR BROW
//     //          break;
//     //        case 2:
//     //          //GREEN CIRCLE FOR JAW, GREEN FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "23";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 3:
//     //          //BLUE CIRCLE FOR JAW, GREEN FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "012";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 4:
//     //          //VIOLET CIRCLE FOR JAW, GREEN FOR BROW
//     //          serialOutEMG.write("1");
//     //          break;
//     //      }
//     //      break;
//     //    case 3:
//     //      //println("Three Brow Raises");
//     //      switch(motorWidgets[0].switchCounter){
//     //        case 1:
//     //          //RED CIRCLE FOR JAW, BLUE FOR BROW
//     //          break;
//     //        case 2:
//     //          //GREEN CIRCLE FOR JAW, BLUE FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "234";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 3:
//     //          //BLUE CIRCLE FOR JAW, BLUE FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "0123";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 4:
//     //          //VIOLET CIRCLE FOR JAW, BLUE FOR BROW
//     //          serialOutEMG.write("2");
//     //          break;
//     //      }
//     //      break;
//     //    case 4:
//     //      //println("Four Brow Raises");
//     //      switch(motorWidgets[0].switchCounter){
//     //        case 1:
//     //          //RED CIRCLE FOR JAW, VIOLET FOR BROW
//     //          break;
//     //        case 2:
//     //          //GREEN CIRCLE FOR JAW, VIOLET FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "0134";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 3:
//     //          //BLUE CIRCLE FOR JAW, VIOLET FOR BROW
//     //          serialOutEMG.write(oldCommand);
//     //          delay(100);
//     //          oldCommand = "01234";
//     //          serialOutEMG.write(oldCommand);
//     //          break;
//     //        case 4:
//     //          //VIOLET CIRCLE FOR JAW, VIOLET FOR BROW
//     //          serialOutEMG.write("3");
//     //          break;
//     //      }
//     //      break;
//     //    case 5:
//     //      //println("Five Brow Raises");
//     //      switch(motorWidgets[0].switchCounter){
//     //        case 1:
//     //          //RED CIRCLE FOR JAW, YELLOW FOR BROW
//     //          break;
//     //        case 2:
//     //          //GREEN CIRCLE FOR JAW, YELLOW FOR BROW
//     //          break;
//     //        case 3:
//     //          //BLUE CIRCLE FOR JAW, YELLOW FOR BROW
//     //          break;
//     //        case 4:
//     //          //VIOLET CIRCLE FOR JAW, YELLOW FOR BROW
//     //          serialOutEMG.write("4");
//     //          break;
//     //      }
//     //      break;
//     //    //case 6:
//     //    //  println("Six Brow Raises");
//     //    //  break;
//     //  }
//     //  motorWidgets[1].switchCounter = 0;
//     //}
//
//
//
//     //----------------- Leftover from Tou Code, what does this do? ----------------------------
//     //OR, you could loop over each EEG channel and do some sort of frequency-domain processing from the FFT data
//     float FFT_freq_Hz, FFT_value_uV;
//     for (int Ichan=0; Ichan < nchan; Ichan++) {
//       //loop over each new sample
//       for (int Ibin=0; Ibin < fftBuff[Ichan].specSize(); Ibin++) {
//         FFT_freq_Hz = fftData[Ichan].indexToFreq(Ibin);
//         FFT_value_uV = fftData[Ichan].getBand(Ibin);
//
//         //add your processing here...
//       }
//     }
//     //---------------------------------------------------------------------------------
//   }
//   void update() {
//
//     //update position/size of the widget
//     x = (int)container[parentContainer].x;
//     y = (int)container[parentContainer].y;
//     w = (int)container[parentContainer].w;
//     h = (int)container[parentContainer].h;
//   }
//
//   void screenResized(PApplet _parent, int _winX, int _winY) {
//     //when screen is resized...
//     //update widget position/size
//     x = (int)container[parentContainer].x;
//     y = (int)container[parentContainer].y;
//     w = (int)container[parentContainer].w;
//     h = (int)container[parentContainer].h;
//
//     configWidget.update(x,y,w,h);
//
//     if(configButton.wasPressed){
//       configButton = new Button(int(x), int(y + h/14), 20, 20, "X", fontInfo.buttonLabel_size);
//       configButton.wasPressed = true;
//     }
//     else{
//       configButton = new Button(int(x), int(y + h/14), 20, 20, "O", fontInfo.buttonLabel_size);
//       configButton.wasPressed = false;
//     }
//
//
//   }
//
//
//   public void draw() {
//     super.draw();
//     if (drawEMG) {
//
//       cp5Serial.setVisible(true);
//
//       pushStyle();
//       noStroke();
//       fill(125);
//       rect(x, y, w, h);
//
//       fill(150, 150, 150);
//       rect(x, y, w, navHeight); //top bar
//       fill(200, 200, 200);
//       rect(x, y+navHeight, w, navHeight); //button bar
//       fill(255);
//       rect(x+2, y+2, navHeight-4, navHeight-4);
//       fill(bgColor, 100);
//       //rect(x+3,y+3, (navHeight-7)/2, navHeight-10);
//       rect(x+4, y+4, (navHeight-10)/2, (navHeight-10)/2);
//       rect(x+4, y+((navHeight-10)/2)+5, (navHeight-10)/2, (navHeight-10)/2);
//       rect(x+((navHeight-10)/2)+5, y+4, (navHeight-10)/2, (navHeight-10)/2);
//       rect(x+((navHeight-10)/2)+5, y+((navHeight-10)/2)+5, (navHeight-10)/2, (navHeight-10 )/2);
//       //text("FFT Plot", x+w/2, y+navHeight/2)
//       fill(bgColor);
//       textAlign(LEFT, CENTER);
//       textFont(f);
//       textSize(18);
//       text("EMG Widget", x+navHeight+2, y+navHeight/2 - 2);
//
//
//       //draw dropdown titles
//       int dropdownPos = 4; //used to loop through drop down titles ... should use for loop with titles in String array, but... laziness has ensued. -Conor
//       int dropdownWidth = 60;
//       textFont(f2);
//       textSize(12);
//       textAlign(CENTER, BOTTOM);
//       fill(bgColor);
//       text("Layout", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//       dropdownPos = 3;
//       text("Headset", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//       //dropdownPos = 3;
//       //text("# Chan.", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//       dropdownPos = 2;
//       text("Polarity", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//       dropdownPos = 1;
//       text("Smoothing", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//       dropdownPos = 0;
//       text("Filters?", x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navHeight-2));
//
//       configButton.draw();
//
//       if (!configButton.wasPressed) {
//         cp5Serial.get(MenuList.class, "serialListConfig").setVisible(false);
//         cp5Serial.get(MenuList.class, "baudList").setVisible(false);
//         float rx = x, ry = y + 2* navHeight, rw = w, rh = h - 2*navHeight;
//         float scaleFactor = 1.0;
//         float scaleFactorJaw = 1.5;
//         int rowNum = 4;
//         int colNum = motorWidgets.length / rowNum;
//         float rowOffset = rh / rowNum;
//         float colOffset = rw / colNum;
//         int index = 0;
//         float currx, curry;
//
//         //new
//         for (int i = 0; i < rowNum; i++) {
//           for (int j = 0; j < colNum; j++) {
//
//             pushMatrix();
//             currx = rx + j * colOffset;
//             curry = ry + i * rowOffset; //never name variables on an empty stomach
//             translate(currx, curry);
//             //draw visualizer
//             noFill();
//             stroke(0, 255, 0);
//             strokeWeight(2);
//             //circle for outer threshold
//             ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].upperThreshold, scaleFactor * motorWidgets[i * colNum + j].upperThreshold);
//             //circle for inner threshold
//             stroke(0, 255, 255);
//             ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold);
//             //realtime
//             fill(255, 0, 0, 125);
//             noStroke();
//             ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].myAverage, scaleFactor * motorWidgets[i * colNum + j].myAverage);
//
//             //draw background bar for mapped uV value indication
//             fill(0, 255, 255, 125);
//             rect(5*colOffset/8, 2 * rowOffset / 8, (3*colOffset/32), int((4*rowOffset/8)));
//
//             //draw real time bar of actually mapped value
//             rect(5*colOffset/8, 6 *rowOffset / 8, (3*colOffset/32), map(motorWidgets[i * colNum + j].output_normalized, 0, 1, 0, (-1) * int((4*rowOffset/8) )));
//
//             //draw thresholds
//             tripSliders[index].update(currx, curry);
//             tripSliders[index].display(5*colOffset/8, 2 * rowOffset / 8, (3*colOffset/32), 2);
//             untripSliders[index].update(currx, curry);
//             untripSliders[index].display(5*colOffset/8, 2 * rowOffset / 8, (3*colOffset/32), 2);
//
//             index++;
//
//             popMatrix();
//           }
//         }
//         popStyle();
//       } else {
//         configWidget.draw();
//       }
//     } else {
//       cp5Serial.setVisible(false);
//     }
//
//     if (serialOutEMG != null) drawTriggerFeedback();
//   } //end of draw
//
//
//
//   //Feedback for triggers/switches.
//   //Currently only used for the OpenBionics implementation, but left
//   //in to give an idea of how it can be used.
//   public void drawTriggerFeedback() {
//     //Is the board streaming data?
//     //if so ... draw feedback
//     if (isRunning) {
//
//       switch (motorWidgets[0].switchCounter) {
//         case 1:
//           fill(255, 0, 0);
//           ellipse(width/2, height - 40, 20, 20);
//           break;
//         case 2:
//           fill(0, 255, 0);
//           ellipse(width/2, height - 40, 20, 20);
//           break;
//         case 3:
//           fill(0, 0, 255);
//           ellipse(width/2, height - 40, 20, 20);
//           break;
//         case 4:
//           fill(128, 0, 128);
//           ellipse(width/2, height - 40, 20, 20);
//           break;
//         }
//     }
//   }
//
//   //Mouse pressed event
//   void mousePressed() {
//     if (mouseX >= x - 35 && mouseX <= x+w && mouseY >= y && mouseY <= y+h && configButton.wasPressed) {
//
//       //Handler for channel selection. No two channels can be
//       //selected at the same time. All values are then set
//       //to whatever value the channel specifies they should
//       //have (particularly analog and digital buttons)
//
//       for (int i = 0; i < nchan; i++) {
//         if (emg_widget.configWidget.chans[i].isMouseHere()) {
//           emg_widget.configWidget.chans[i].setIsActive(true);
//           emg_widget.configWidget.chans[i].wasPressed = true;
//           lastChan = i;
//
//           if (!motorWidgets[lastChan].digitalBool) {
//             emg_widget.configWidget.digital.setIsActive(false);
//           } else if (motorWidgets[lastChan].digitalBool) {
//             emg_widget.configWidget.digital.setIsActive(true);
//           }
//
//           if (!motorWidgets[lastChan].analogBool) {
//             emg_widget.configWidget.analog.setIsActive(false);
//           } else if (motorWidgets[lastChan].analogBool) {
//             emg_widget.configWidget.analog.setIsActive(true);
//           }
//
//           break;
//         }
//       }
//
//       //Digital button event
//       if (emg_widget.configWidget.digital.isMouseHere()) {
//         if (emg_widget.configWidget.digital.wasPressed) {
//           motorWidgets[lastChan].digitalBool = false;
//           emg_widget.configWidget.digital.wasPressed = false;
//           emg_widget.configWidget.digital.setIsActive(false);
//         } else if (!emg_widget.configWidget.digital.wasPressed) {
//           motorWidgets[lastChan].digitalBool = true;
//           emg_widget.configWidget.digital.wasPressed = true;
//           emg_widget.configWidget.digital.setIsActive(true);
//         }
//       }
//
//       //Analog button event
//       if (emg_widget.configWidget.analog.isMouseHere()) {
//         if (emg_widget.configWidget.analog.wasPressed) {
//           motorWidgets[lastChan].analogBool = false;
//           emg_widget.configWidget.analog.wasPressed = false;
//           emg_widget.configWidget.analog.setIsActive(false);
//         } else if (!emg_widget.configWidget.analog.wasPressed) {
//           motorWidgets[lastChan].analogBool = true;
//           emg_widget.configWidget.analog.wasPressed = true;
//           emg_widget.configWidget.analog.setIsActive(true);
//         }
//       }
//
//       //Connect button event
//       if (emg_widget.configWidget.connectToSerial.isMouseHere()) {
//         emg_widget.configWidget.connectToSerial.wasPressed = true;
//         emg_widget.configWidget.connectToSerial.setIsActive(true);
//       }
//     } else if (mouseX >= (x) && mouseX <= (x-20) && mouseY >= y && mouseY <= y+20) {
//
//       //Close button stuff
//       if(mouseX >= x && mouseX <= (x+20) && mouseY >= y + h/14 && mouseY <= y + h/14 + 20){
//         configButton.wasPressed = false;
//         configButton.setString("O");
//       }
//     } else if (mouseX >= (x) && mouseX <= (x+20) && mouseY >= y + h/14 && mouseY <= y+ h/14 + 20) {
//
//       //Open configuration menu
//       if (configButton.isMouseHere()) {
//         configButton.setIsActive(true);
//         //configButton = new Button(int(x), int(y + h/14), 20, 20, "O", fontInfo.buttonLabel_size);
//
//
//         if (configButton.wasPressed) {
//           //edge case, sometimes this is needed
//           configButton.wasPressed = false;
//           configButton.setString("O");
//         } else {
//           configButton.wasPressed = true;
//           configButton.setString("X");
//         }
//       }
//     }
//   }
//
//   //Mouse Released Event
//   void mouseReleased() {
//     // println("EMG_Widget: mouseReleased: nchan " + nchan);
//     for (int i = 0; i < nchan; i++) {
//       if (!emg_widget.configWidget.dynamicThreshold.wasPressed && !configButton.wasPressed) {
//         tripSliders[i].releaseEvent();
//         untripSliders[i].releaseEvent();
//       }
//
//       if (i != lastChan) {
//         emg_widget.configWidget.chans[i].setIsActive(false);
//         emg_widget.configWidget.chans[i].wasPressed = false;
//       }
//     }
//
//     if (emg_widget.configWidget.connectToSerial.isMouseHere()) {
//       emg_widget.configWidget.connectToSerial.wasPressed = false;
//       emg_widget.configWidget.connectToSerial.setIsActive(false);
//
//       try {
//         serialOutEMG = new Serial(parent, serialNameEMG, Integer.parseInt(baudEMG));
//         emg_widget.configWidget.print_onscreen("Connected!");
//       }
//       catch (Exception e) {
//         emg_widget.configWidget.print_onscreen("Could not connect!");
//       }
//     }
//
//     configButton.setIsActive(false);
//   }
//
//
//   //=============== Config_Widget ================
//   //=  The configuration menu. Customize in any  =
//   //=  way that could help you out!              =
//   //=                                            =
//   //=  TODO: Add dynamic threshold functionality =
//   //==============================================
//
//   class Config_Widget extends Container {
//     private float fs_Hz;
//     private int nchan;
//     private Motor_Widget[] parent;
//     public Button[] chans;
//     public Button analog;
//     public Button digital;
//     public Button valueThreshold;
//     public Button dynamicThreshold;
//     public Button connectToSerial;
//     Container c;
//
//     MenuList serialListLocal;
//     MenuList baudList;
//     String last_message = "";
//     String[] serialPortsLocal = new String[Serial.list().length];
//
//
//     //Constructor
//     public Config_Widget(int NCHAN, float sample_rate_Hz, Container container, Motor_Widget[] parent) {
//       super(container, "WHOLE");
//       c = container;
//
//       // println("EMG_Widget: Config_Widget: nchan " + NCHAN);
//
//       this.nchan = NCHAN;
//       this.fs_Hz = sample_rate_Hz;
//       this.parent = parent;
//       nchan = NCHAN;
//
//       chans = new Button[NCHAN];
//       digital = new Button(int(x + w/7.5), int(y + h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       analog = new Button(int(x - w/27.3), int(y + h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       valueThreshold = new Button(int(x+ w/1.74), int(y+ h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       dynamicThreshold = new Button(int(x+w/2.73), int(y+h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);  //CURRENTLY DOES NOTHING! Working on implementation
//       connectToSerial = new Button(int(x+w/1.74), int(y+h/1.16), int(w/4.096), int(w/13.824), "Connect", 18);
//
//       digital.setIsActive(true);
//       digital.wasPressed = true;
//       analog.setIsActive(true);
//       analog.wasPressed = true;
//       valueThreshold.setIsActive(true);
//       valueThreshold.wasPressed = true;
//
//       //Available serial outputs
//       serialListLocal = new MenuList(cp5Serial, "serialListConfig", int(w/1.74), int(h/2.88), f2);
//       serialListLocal.setPosition(x - w/40.96, y + h/2.56);
//       serialPortsLocal = Serial.list();
//       for (int i = 0; i < serialPortsLocal.length; i++) {
//         String tempPort = serialPortsLocal[(serialPortsLocal.length-1) - i]; //list backwards... because usually our port is at the bottom
//         if (!tempPort.equals(openBCI_portName)) serialListLocal.addItem(makeItem(tempPort));
//       }
//
//       //List of BAUD values
//       baudList = new MenuList(cp5Serial, "baudList", int(w/4.096), int(h/2.88), f2);
//       baudList.setPosition(x+w/1.74, y + h/2.16);
//
//       baudList.addItem(makeItem("230400"));
//       baudList.addItem(makeItem("115200"));
//       baudList.addItem(makeItem("57600"));
//       baudList.addItem(makeItem("38400"));
//       baudList.addItem(makeItem("28800"));
//       baudList.addItem(makeItem("19200"));
//       baudList.addItem(makeItem("14400"));
//       baudList.addItem(makeItem("9600"));
//       baudList.addItem(makeItem("7200"));
//       baudList.addItem(makeItem("4800"));
//       baudList.addItem(makeItem("3600"));
//       baudList.addItem(makeItem("2400"));
//       baudList.addItem(makeItem("1800"));
//       baudList.addItem(makeItem("1200"));
//       baudList.addItem(makeItem("600"));
//       baudList.addItem(makeItem("300"));
//
//
//       //Set first items to active
//       Map bob = ((MenuList)baudList).getItem(0);
//       baudEMG = (String)bob.get("headline");
//       baudList.activeItem = 0;
//
//       Map bobSer = ((MenuList)serialListLocal).getItem(0);
//       serialNameEMG = (String)bobSer.get("headline");
//       serialListLocal.activeItem = 0;
//
//       //Hide the list until open button clicked
//       cp5Serial.get(MenuList.class, "serialListConfig").setVisible(false);
//       cp5Serial.get(MenuList.class, "baudList").setVisible(false);
//
//       //Buttons for different channels (Just displays number if 16 channel)
//       for (int i = 0; i < NCHAN; i++) {
//         if (NCHAN == 8) chans[i] = new Button(int(x - w/13.65 + (i * (w-w/40.96)/nchan )), int(y + w/40.96), int((w-w/40.96)/nchan), 30, "CHAN " + (i+1), fontInfo.buttonLabel_size);
//         else chans[i] = new Button(int(x - w/13.65 + (i * (w-w/40.96)/nchan )), int(y + h/69.12), int((w-w/40.96)/nchan), int(w/13.65), "" + (i+1), fontInfo.buttonLabel_size);
//       }
//
//       //Set fist channel as active
//       chans[0].setIsActive(true);
//       chans[0].wasPressed = true;
//     }
//     public void update(float lx, float ly, float lw, float lh){
//
//       x = lx + x/6.12;
//       y = ly + y/3.03;
//       w = lw - w/4.25;
//       h = lh - h/245.1;
//
//
//       chans = new Button[nchan];
//       digital = new Button(int(x + w/7.5), int(y + h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       analog = new Button(int(x - w/27.3), int(y + h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       valueThreshold = new Button(int(x+ w/1.74), int(y+ h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);
//       dynamicThreshold = new Button(int(x+w/2.73), int(y+h/5.77), int(w/40.96), int(w/40.96), "", fontInfo.buttonLabel_size);  //CURRENTLY DOES NOTHING! Working on implementation
//       connectToSerial = new Button(int(x+w/1.74), int(y+h/1.16), int(w/4.096), int(w/13.824), "Connect", 18);
//
//       digital.setIsActive(true);
//       digital.wasPressed = true;
//       analog.setIsActive(true);
//       analog.wasPressed = true;
//       valueThreshold.setIsActive(true);
//       valueThreshold.wasPressed = true;
//
//       //Available serial outputs
//       serialListLocal = new MenuList(cp5Serial, "serialListConfig", int(w/1.74), int(h/2.88), f2);
//       serialListLocal.setPosition(x - w/40.96, y + h/2.56);
//
//       serialPortsLocal = Serial.list();
//       for (int i = 0; i < serialPortsLocal.length; i++) {
//         String tempPort = serialPortsLocal[(serialPortsLocal.length-1) - i]; //list backwards... because usually our port is at the bottom
//         if (!tempPort.equals(openBCI_portName)) serialListLocal.addItem(makeItem(tempPort));
//       }
//
//       //List of BAUD values
//       baudList = new MenuList(cp5Serial, "baudList", int(w/4.096), int(h/2.88), f2);
//       baudList.setPosition(x+w/1.74, y + h/2.16);
//
//       baudList.addItem(makeItem("230400"));
//       baudList.addItem(makeItem("115200"));
//       baudList.addItem(makeItem("57600"));
//       baudList.addItem(makeItem("38400"));
//       baudList.addItem(makeItem("28800"));
//       baudList.addItem(makeItem("19200"));
//       baudList.addItem(makeItem("14400"));
//       baudList.addItem(makeItem("9600"));
//       baudList.addItem(makeItem("7200"));
//       baudList.addItem(makeItem("4800"));
//       baudList.addItem(makeItem("3600"));
//       baudList.addItem(makeItem("2400"));
//       baudList.addItem(makeItem("1800"));
//       baudList.addItem(makeItem("1200"));
//       baudList.addItem(makeItem("600"));
//       baudList.addItem(makeItem("300"));
//
//
//       //Set first items to active
//       Map bob = ((MenuList)baudList).getItem(0);
//       baudEMG = (String)bob.get("headline");
//       baudList.activeItem = 0;
//
//       Map bobSer = ((MenuList)serialListLocal).getItem(0);
//       serialNameEMG = (String)bobSer.get("headline");
//       serialListLocal.activeItem = 0;
//
//       //Hide the list until open button clicked
//       cp5Serial.get(MenuList.class, "serialListConfig").setVisible(false);
//       cp5Serial.get(MenuList.class, "baudList").setVisible(false);
//
//       //Buttons for different channels (Just displays number if 16 channel)
//       for (int i = 0; i < nchan; i++) {
//         if (nchan == 8) chans[i] = new Button(int(x - w/13.65 + (i * (w-w/40.96)/nchan )), int(y + w/40.96), int((w-w/40.96)/nchan), 30, "CHAN " + (i+1), fontInfo.buttonLabel_size);
//         else chans[i] = new Button(int(x - w/13.65 + (i * (w-w/40.96)/nchan )), int(y + h/69.12), int((w-w/40.96)/nchan), int(w/13.65), "" + (i+1), fontInfo.buttonLabel_size);
//
//       }
//
//       //Set fist channel as active
//       chans[0].setIsActive(true);
//       chans[0].wasPressed = true;
//     }
//
//     public void draw() {
//       pushStyle();
//
//       float rx = x, ry = y, rw = w, rh =h;
//       //println("x: " + rx + " y: " + ry + " w: " + rw + " h: " + rh);
//       //Config Window Rectangle
//       fill(211, 211, 211);
//       rect(rx - w/11.7, ry, rw, rh);
//
//       //Serial Config Rectangle
//       fill(190, 190, 190);
//       rect(rx - w/13.65, ry+h/3.84, rw- w/40.96, rh-h/3.64);
//
//
//       //Channel Configs
//       fill(255, 255, 255);
//       for (int i = 0; i < nchan; i++) {
//         chans[i].draw();
//       }
//       drawAnalogSelection();
//       drawThresholdSelection();
//       drawMenuLists();
//
//       print_lastmessage();
//     }
//
//     void drawAnalogSelection() {
//       fill(233, 233, 233);
//       rect(x-w/13.65, y+h/6.91, w/2.48, h/11.52);
//       analog.draw();
//       digital.draw();
//       fill(50);
//       text("Analog", x+w/20.48, y+h/5.49);
//       text("Digital", x+w/4.55, y+h/5.49);
//
//     }
//
//     void drawThresholdSelection() {
//       fill(233, 233, 233);
//       rect(x+w/2.93, y+h/6.91, w/1.78, h/11.52);
//       valueThreshold.draw();
//       dynamicThreshold.draw();
//
//       fill(50);
//       textAlign(LEFT);
//       textSize(13);
//       text("Dynamic", x+w/2.45, y+h/5.08);
//       text("Trip Value     %" + (double)Math.round((parent[lastChan].tripThreshold * 100) * 10d) / 10d, x+w/1.64, y+h/5.49);
//       text("Untrip Value %"+ (double)Math.round((parent[lastChan].untripThreshold * 100) * 10d) / 10d, x+w/1.64, y+h/4.43);
//
//     }
//
//     void drawMenuLists() {
//       fill(50);
//       textFont(f1);
//       textAlign(CENTER);
//       textSize(18);
//       text("Serial Out Configuration", x+w/2.56, y+h/2.88);
//
//       textSize(14);
//       textAlign(LEFT);
//       text("Serial Port", x-w/40.96, y + h/2.3);
//       text("BAUD Rate", x+w/1.74, y+h/2.3);
//       cp5Serial.get(MenuList.class, "serialListConfig").setVisible(true); //make sure the serialList menulist is visible
//       cp5Serial.get(MenuList.class, "baudList").setVisible(true); //make sure the baudList menulist is visible
//
//       connectToSerial.draw();
//     }
//
//     public void print_onscreen(String localstring) {
//       textAlign(LEFT);
//       fill(0);
//       rect(x - w/40.96, y + h/1.19, (w-w/2.34), h/8.64);
//       fill(255);
//       text(localstring, x, y + h/1.13, ( w - w/2.28), h/13.82);
//
//       this.last_message = localstring;
//     }
//
//     void print_lastmessage() {
//       textAlign(LEFT);
//       fill(0);
//       rect(x - w/40.96, y + h/1.19, (w-w/2.34), h/8.64);
//       fill(255);
//       text(this.last_message, x, y + h/1.13, ( w - w/2.28), h/13.82);
//
//     }
//   }
//
//
//
//   //============= TripSlider =============
//   //=  Class for moving thresholds. Can  =
//   //=  be dragged up and down, but lower =
//   //=  thresholds cannot go above upper  =
//   //=  thresholds (and visa versa).      =
//   //======================================
//   class TripSlider {
//     //Fields
//     int lx, ly;
//     int boxx, boxy;
//     int stretch;
//     int wid;
//     int len;
//     boolean over;
//     boolean press;
//     boolean locked = false;
//     boolean otherslocked = false;
//     boolean trip;
//     boolean drawHand;
//     TripSlider[] others;
//     color current_color = color(255, 255, 255);
//     Motor_Widget parent;
//
//     //Constructor
//     TripSlider(int ix, int iy, int il, int iwid, int ilen, TripSlider[] o, boolean wastrip, Motor_Widget p) {
//       lx = ix;
//       ly = iy;
//       stretch = il;
//       wid = iwid;
//       len = ilen;
//       boxx = lx - wid/2;
//       //boxx = lx;
//       boxy = ly-stretch - len/2;
//       //boxy = ly;
//       others = o;
//       trip = wastrip;  //Boolean to distinguish between trip and untrip thresholds
//       parent = p;
//     }
//
//     //Called whenever thresholds are dragged
//     void update(float tx, float ty) {
//       boxx = lx;
//       boxy = (wid + (ly/2)) - int(((wid + (ly/2)) - ly) * (float(stretch) / float(wid)));
//       //boxy = ly + (ly - int( ly * (float(stretch) / float(wid)))) ;
//
//       for (int i=0; i<others.length; i++) {
//         if (others[i].locked == true) {
//           otherslocked = true;
//           break;
//         } else {
//           otherslocked = false;
//         }
//       }
//
//       if (otherslocked == false) {
//         overEvent(tx, ty);
//         pressEvent();
//       }
//
//       if (press) {
//         //Some of this may need to be refactored in order to support window resizing
//         int mappedVal = int(map((mouseY - (ty + ly) ), ((ty+ly) + wid - (ly/2)) - (ty+ly), 0, 0, wid));
//         println("ty: " + ty + " ly: " + ly + " mouseY: " + mouseY + " boxy: " + boxy + " stretch: " + stretch + " width: " + wid);
//         if (trip) stretch = lock(mappedVal, int(parent.untripThreshold * (wid)), wid);
//         else stretch =  lock(mappedVal, 0, int(parent.tripThreshold * (wid)));
//
//         if (mappedVal > wid && trip) parent.tripThreshold = 1;
//         else if (mappedVal > wid && !trip) parent.untripThreshold = 1;
//         else if (mappedVal < 0 && trip) parent.tripThreshold = 0;
//         else if (mappedVal < 0 && !trip) parent.untripThreshold = 0;
//         else if (trip) parent.tripThreshold = float(mappedVal) / (wid);
//         else if (!trip) parent.untripThreshold = float(mappedVal) / (wid);
//       }
//     }
//
//     //Checks if mouse is here
//     void overEvent(float tx, float ty) {
//       if (overRect(int(boxx + tx), int(boxy + ty), wid, len)) {
//         over = true;
//       } else {
//         over = false;
//       }
//     }
//
//     //Checks if mouse is pressed
//     void pressEvent() {
//       if (over && mousePressed || locked) {
//         press = true;
//         locked = true;
//       } else {
//         press = false;
//       }
//     }
//
//     //Mouse was released
//     void releaseEvent() {
//       locked = false;
//     }
//
//     //Color selector and cursor setter
//     void setColor() {
//       if (over) {
//         current_color = color(127, 134, 143);
//         if (!drawHand) {
//           cursor(HAND);
//           drawHand = true;
//         }
//       } else {
//         if (trip) current_color = color(0, 255, 0);
//         else current_color = color(255, 0, 0);
//         if (drawHand) {
//           cursor(ARROW);
//           drawHand = false;
//         }
//       }
//     }
//
//     //Helper function to make setting default threshold values easier.
//     //Expects a float as input (0.25 is 25%)
//     void setStretchPercentage(float val) {
//       stretch = lock(int((wid) * val), 0, wid);
//     }
//
//     //Displays the thresholds
//     void display(float tx, float ty, float tw, float tl) {
//       lx = int(tx);
//       ly = int(ty);
//       wid = int(tw);
//       len = int(tl);
//
//       fill(255);
//       strokeWeight(0);
//       stroke(255);
//       setColor();
//       fill(current_color);
//       rect(boxx, boxy, wid, len);
//
//       //rect(lx, ly, wid, len);
//     }
//
//     //Check if the mouse is here
//     boolean overRect(int lx, int ly, int twidth, int theight) {
//       if (mouseX >= lx && mouseX <= lx+twidth &&
//         mouseY >= ly && mouseY <= ly+theight) {
//
//         return true;
//       } else {
//         return false;
//       }
//     }
//
//     //Locks the threshold in place
//     int lock(int val, int minv, int maxv) {
//       return  min(max(val, minv), maxv);
//     }
//   }
//
//
//
//   //===================== DIGITAL EVENTS =============================
//   //=  Digital Events work by tripping certain thresholds, and then  =
//   //=  untripping said thresholds. In order to use digital events    =
//   //=  you will need to observe the switchCounter field in any       =
//   //=  given channel. Check out the OpenBionics Switch Example       =
//   //=  in the process() function above to get an idea of how to do   =
//   //=  this. It is important that your observation of switchCounter  =
//   //=  is done in the process() function AFTER the Digital Events    =
//   //=  are evoked.                                                   =
//   //=                                                                =
//   //=  This system supports both digital and analog events           =
//   //=  simultaneously and seperated.                                 =
//   //==================================================================
//
//   //Channel 1 Event
//   void digitalEventChan0(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (motorWidgets[0].switchCounter > 4) motorWidgets[0].switchCounter = 0;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 2 Event
//   void digitalEventChan1(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 3 Event
//   void digitalEventChan2(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 4 Event
//   void digitalEventChan3(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 5 Event
//   void digitalEventChan4(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 6 Event
//   void digitalEventChan5(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 7 Event
//   void digitalEventChan6(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 8 Event
//   void digitalEventChan7(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 9 Event
//   void digitalEventChan8(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 10 Event
//   void digitalEventChan9(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 11 Event
//   void digitalEventChan10(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 12 Event
//   void digitalEventChan11(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 13 Event
//   void digitalEventChan12(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 14 Event
//   void digitalEventChan13(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 15 Event
//   void digitalEventChan14(Motor_Widget cfc) {
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//   //Channel 16 Event
//   void digitalEventChan15(Motor_Widget cfc) {
//
//     //Local instances of Motor_Widget fields
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//     //Custom waiting threshold
//     int timeToWaitThresh = 750;
//
//     if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
//       //Tripped
//       cfc.switchTripped = true;
//       cfc.timeOfLastTrip = millis();
//       cfc.switchCounter++;
//     }
//     if (switchTripped && output_normalized <= untripThreshold) {
//       //Untripped
//       cfc.switchTripped = false;
//     }
//   }
//
//
//   //===================== ANALOG EVENTS ===========================
//   //=  Analog events are a big more complicated than digital      =
//   //=  events. In order to use analog events you must map the     =
//   //=  output_normalized value to whatver minimum and maximum     =
//   //=  you'd like and then write that to the serialOutEMG.        =
//   //=                                                             =
//   //=  Check out analogEventChan0() for the OpenBionics analog    =
//   //=  event example to get an idea of how to use analog events.  =
//   //===============================================================
//
//   //Channel 1 Event
//   void analogEventChan0(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//
//
//     //================= OpenBionics Analog Movement Example =======================
//     if (serialOutEMG != null) {
//       //println("Output normalized: " + int(map(output_normalized, 0, 1, 0, 100)));
//       if (int(map(output_normalized, 0, 1, 0, 100)) > 10) {
//         serialOutEMG.write("G0P" + int(map(output_normalized, 0, 1, 0, 100)));
//         delay(10);
//       } else serialOutEMG.write("G0P0");
//     }
//   }
//
//   //Channel 2 Event
//   void analogEventChan1(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 3 Event
//   void analogEventChan2(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 4 Event
//   void analogEventChan3(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 5 Event
//   void analogEventChan4(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 6 Event
//   void analogEventChan5(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 7 Event
//   void analogEventChan6(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 8 Event
//   void analogEventChan7(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 9 Event
//   void analogEventChan8(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 10 Event
//   void analogEventChan9(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 11 Event
//   void analogEventChan10(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 12 Event
//   void analogEventChan11(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 13 Event
//   void analogEventChan12(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 14 Event
//   void analogEventChan13(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 15 Event
//   void analogEventChan14(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
//
//   //Channel 16 Event
//   void analogEventChan15(Motor_Widget cfc) {
//
//     float output_normalized = cfc.output_normalized;
//     float tripThreshold = cfc.tripThreshold;
//     float untripThreshold = cfc.untripThreshold;
//     boolean switchTripped = cfc.switchTripped;
//     float timeOfLastTrip = cfc.timeOfLastTrip;
//   }
// }

//////////////////////////////////////
//
// This file contains classes that are helfpul in some way.
// Created: Chip Audette, Oct 2013 - Dec 2014
//
/////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//////////////////////////////////////////////////
//
// Formerly, Math.pde
//  - std
//  - mean
//  - medianDestructive
//  - findMax
//  - mean
//  - sum
//  - CalcDotProduct
//  - log10
//  - filterWEA_1stOrderIIR
//  - filterIIR
//  - removeMean
//  - rereferenceTheMontage
//  - CLASS RunningMean
//
// Created: Chip Audette, Oct 2013
//
//////////////////////////////////////////////////

//compute the standard deviation
public float std(float[] data) {
  //calc mean
  float ave = mean(data);

  //calc sum of squares relative to mean
  float val = 0;
  for (int i=0; i < data.length; i++) {
    val += pow(data[i]-ave,2);
  }

  // divide by n to make it the average
  val /= data.length;

  //take square-root and return the standard
  return (float)Math.sqrt(val);
}


public float mean(float[] data) {
  return mean(data,data.length);
}

public int medianDestructive(int[] data) {
  sort(data);
  int midPoint = data.length / 2;
  return data[midPoint];
}

//////////////////////////////////////////////////
//
// Some functions to implement some math and some filtering.  These functions
// probably already exist in Java somewhere, but it was easier for me to just
// recreate them myself as I needed them.
//
// Created: Chip Audette, Oct 2013
//
//////////////////////////////////////////////////

public int findMax(float[] data) {
  float maxVal = data[0];
  int maxInd = 0;
  for (int I=1; I<data.length; I++) {
    if (data[I] > maxVal) {
      maxVal = data[I];
      maxInd = I;
    }
  }
  return maxInd;
}

public float mean(float[] data, int Nback) {
  return sum(data,Nback)/Nback;
}

public float sum(float[] data) {
  return sum(data, data.length);
}

public float sum(float[] data, int Nback) {
  float sum = 0;
  if (Nback > 0) {
    for (int i=(data.length)-Nback; i < data.length; i++) {
      sum += data[i];
    }
  }
  return sum;
}

public float calcDotProduct(float[] data1, float[] data2) {
  int len = min(data1.length, data2.length);
  float val=0.0f;
  for (int I=0;I<len;I++) {
    val+=data1[I]*data2[I];
  }
  return val;
}


public float log10(float val) {
  return (float)Math.log10(val);
}

public float filterWEA_1stOrderIIR(float[] filty, float learn_fac, float filt_state) {
  float prev = filt_state;
  for (int i=0; i < filty.length; i++) {
    filty[i] = prev*(1-learn_fac) + filty[i]*learn_fac;
    prev = filty[i]; //save for next time
  }
  return prev;
}

public void filterIIR(double[] filt_b, double[] filt_a, float[] data) {
  int Nback = filt_b.length;
  double[] prev_y = new double[Nback];
  double[] prev_x = new double[Nback];

  //step through data points
  for (int i = 0; i < data.length; i++) {
    //shift the previous outputs
    for (int j = Nback-1; j > 0; j--) {
      prev_y[j] = prev_y[j-1];
      prev_x[j] = prev_x[j-1];
    }

    //add in the new point
    prev_x[0] = data[i];

    //compute the new data point
    double out = 0;
    for (int j = 0; j < Nback; j++) {
      out += filt_b[j]*prev_x[j];
      if (j > 0) {
        out -= filt_a[j]*prev_y[j];
      }
    }

    //save output value
    prev_y[0] = out;
    data[i] = (float)out;
  }
}


public void removeMean(float[] filty, int Nback) {
  float meanVal = mean(filty,Nback);
  for (int i=0; i < filty.length; i++) {
    filty[i] -= meanVal;
  }
}

public void rereferenceTheMontage(float[][] data) {
  int n_chan = data.length;
  int n_points = data[0].length;
  float sum, mean;

  //loop over all data points
  for (int Ipoint=0;Ipoint<n_points;Ipoint++) {
    //compute mean signal right now
    sum=0.0f;
    for (int Ichan=0;Ichan<n_chan;Ichan++) sum += data[Ichan][Ipoint];
    mean = sum / n_chan;

    //remove the mean signal from all channels
    for (int Ichan=0;Ichan<n_chan;Ichan++) data[Ichan][Ipoint] -= mean;
  }
}

//------------------------------------------------------------------------
//                            Classes
//------------------------------------------------------------------------

class RunningMean {
  private float[] values;
  private int cur_ind = 0;
  RunningMean(int N) {
    values = new float[N];
    cur_ind = 0;
  }
  public void addValue(float val) {
    values[cur_ind] = val;
    cur_ind = (cur_ind + 1) % values.length;
  }
  public float calcMean() {
    return mean(values);
  }
};

class DataPacket_ADS1299 {
  private final int rawAdsSize = 3;
  private final int rawAuxSize = 2;

  int sampleIndex;
  int[] values;
  int[] auxValues;
  byte[][] rawValues;
  byte[][] rawAuxValues;

  //constructor, give it "nValues", which should match the number of values in the
  //data payload in each data packet from the Arduino.  This is likely to be at least
  //the number of EEG channels in the OpenBCI system (ie, 8 channels if a single OpenBCI
  //board) plus whatever auxiliary data the Arduino is sending.
  DataPacket_ADS1299(int nValues, int nAuxValues) {
    values = new int[nValues];
    auxValues = new int[nAuxValues];
    rawValues = new byte[nValues][rawAdsSize];
    rawAuxValues = new byte[nAuxValues][rawAdsSize];
  }
  public int printToConsole() {
    print("printToConsole: DataPacket = ");
    print(sampleIndex);
    for (int i=0; i < values.length; i++) {
      print(", " + values[i]);
    }
    for (int i=0; i < auxValues.length; i++) {
      print(", " + auxValues[i]);
    }
    println();
    return 0;
  }

  public int copyTo(DataPacket_ADS1299 target) { return copyTo(target, 0, 0); }
  public int copyTo(DataPacket_ADS1299 target, int target_startInd_values, int target_startInd_aux) {
    target.sampleIndex = sampleIndex;
    return copyValuesAndAuxTo(target, target_startInd_values, target_startInd_aux);
  }
  public int copyValuesAndAuxTo(DataPacket_ADS1299 target, int target_startInd_values, int target_startInd_aux) {
    int nvalues = values.length;
    for (int i=0; i < nvalues; i++) {
      target.values[target_startInd_values + i] = values[i];
      target.rawValues[target_startInd_values + i] = rawValues[i];
    }
    nvalues = auxValues.length;
    for (int i=0; i < nvalues; i++) {
      target.auxValues[target_startInd_aux + i] = auxValues[i];
      target.rawAuxValues[target_startInd_aux + i] = rawAuxValues[i];
    }
    return 0;
  }
};

class DataStatus {
  public boolean is_railed;
  private int threshold_railed;
  public boolean is_railed_warn;
  private int threshold_railed_warn;

  DataStatus(int thresh_railed, int thresh_railed_warn) {
    is_railed = false;
    threshold_railed = thresh_railed;
    is_railed_warn = false;
    threshold_railed_warn = thresh_railed_warn;
  }
  public void update(int data_value) {
    is_railed = false;
    if (abs(data_value) >= threshold_railed) is_railed = true;
    is_railed_warn = false;
    if (abs(data_value) >= threshold_railed_warn) is_railed_warn = true;
  }
};

class FilterConstants {
  public double[] a;
  public double[] b;
  public String name;
  public String short_name;
  FilterConstants(double[] b_given, double[] a_given, String name_given, String short_name_given) {
    b = new double[b_given.length];a = new double[b_given.length];
    for (int i=0; i<b.length;i++) { b[i] = b_given[i];}
    for (int i=0; i<a.length;i++) { a[i] = a_given[i];}
    name = name_given;
    short_name = short_name_given;
  }
};

class DetectionData_FreqDomain {
  public float inband_uV = 0.0f;
  public float inband_freq_Hz = 0.0f;
  public float guard_uV = 0.0f;
  public float thresh_uV = 0.0f;
  public boolean isDetected = false;

  DetectionData_FreqDomain() {
  }
};

class GraphDataPoint {
  public double x;
  public double y;
  public String x_units;
  public String y_units;
};

class PlotFontInfo {
    String fontName = "fonts/Raleway-Regular.otf";
    int axisLabel_size = 16;
    int tickLabel_size = 14;
    int buttonLabel_size = 12;
};

class TextBox {
  public int x, y;
  public int textColor;
  public int backgroundColor;
  private PFont font;
  private int fontSize;
  public String string;
  public boolean drawBackground;
  public int backgroundEdge_pixels;
  public int alignH,alignV;

//  textBox(String s,int x1,int y1) {
//    textBox(s,x1,y1,0);
//  }
  TextBox(String s, int x1, int y1) {
    string = s; x = x1; y = y1;
    backgroundColor = color(255,255,255);
    textColor = color(0,0,0);
    fontSize = 12;
    font = p5;
    backgroundEdge_pixels = 1;
    drawBackground = false;
    alignH = LEFT;
    alignV = BOTTOM;
  }
  public void setFontSize(int size) {
    fontSize = size;
    font = p5;
  }
  public void draw() {
    //define text
    noStroke();
    textFont(font);

    //draw the box behind the text
    if (drawBackground == true) {
      int w = PApplet.parseInt(round(textWidth(string)));
      int xbox = x - backgroundEdge_pixels;
      switch (alignH) {
        case LEFT:
          xbox = x - backgroundEdge_pixels;
          break;
        case RIGHT:
          xbox = x - w - backgroundEdge_pixels;
          break;
        case CENTER:
          xbox = x - PApplet.parseInt(round(w/2.0f)) - backgroundEdge_pixels;
          break;
      }
      w = w + 2*backgroundEdge_pixels;
      int h = PApplet.parseInt(textAscent())+2*backgroundEdge_pixels;
      int ybox = y - PApplet.parseInt(round(textAscent())) - backgroundEdge_pixels -2;
      fill(backgroundColor);
      rect(xbox,ybox,w,h);
    }
    //draw the text itself
    fill(textColor);
    textAlign(alignH,alignV);
    text(string,x,y);
    strokeWeight(1);
  }
};
///////////////////////////////////////////////////////////////////////////////
//
// This class configures and manages the connection to the OpenBCI Ganglion.
// The connection is implemented via a TCP connection to a TCP port.
// The Gagnlion is configured using single letter text commands sent from the
// PC to the TCP server.  The EEG data streams back from the Ganglion, to the
// TCP server and back to the PC continuously (once started).
//
// Created: AJ Keller, August 2016
//
/////////////////////////////////////////////////////////////////////////////

// import java.io.OutputStream; //for logging raw bytes to an output file

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

boolean werePacketsDroppedGang = false;
int numPacketsDroppedGang = 0;

public void clientEvent(Client someClient) {
  // print("Server Says:  ");

  int p = ganglion.tcpBufferPositon;
  ganglion.tcpBuffer[p] = ganglion.tcpClient.readChar();
  ganglion.tcpBufferPositon++;

  if(p > 2) {
    String posMatch  = new String(ganglion.tcpBuffer, p - 2, 3);
    if (posMatch.equals(ganglion.TCP_STOP)) {
      if (!ganglion.nodeProcessHandshakeComplete) {
        ganglion.nodeProcessHandshakeComplete = true;
        ganglion.setHubIsRunning(true);
        println("GanglionSync: clientEvent: handshake complete");
      }
      // Get a string from the tcp buffer
      String msg = new String(ganglion.tcpBuffer, 0, p);
      // Send the new string message to be processed
      ganglion.parseMessage(msg);
      // Check to see if the ganglion ble list needs to be updated
      if (ganglion.deviceListUpdated) {
        ganglion.deviceListUpdated = false;
        controlPanel.bleBox.refreshBLEList();
      }
      // Reset the buffer position
      ganglion.tcpBufferPositon = 0;
    }
  }
}

class OpenBCI_Ganglion {
  final static String TCP_CMD_ACCEL = "a";
  final static String TCP_CMD_CONNECT = "c";
  final static String TCP_CMD_COMMAND = "k";
  final static String TCP_CMD_DISCONNECT = "d";
  final static String TCP_CMD_DATA= "t";
  final static String TCP_CMD_ERROR = "e";
  final static String TCP_CMD_IMPEDANCE = "i";
  final static String TCP_CMD_LOG = "l";
  final static String TCP_CMD_SCAN = "s";
  final static String TCP_CMD_STATUS = "q";
  final static String TCP_STOP = ",;\n";

  final static String TCP_ACTION_START = "start";
  final static String TCP_ACTION_STATUS = "status";
  final static String TCP_ACTION_STOP = "stop";

  final static String GANGLION_BOOTLOADER_MODE = ">";

  final static byte BYTE_START = (byte)0xA0;
  final static byte BYTE_END = (byte)0xC0;

  // States For Syncing with the hardware
  final static int STATE_NOCOM = 0;
  final static int STATE_COMINIT = 1;
  final static int STATE_SYNCWITHHARDWARE = 2;
  final static int STATE_NORMAL = 3;
  final static int STATE_STOPPED = 4;
  final static int COM_INIT_MSEC = 3000; //you may need to vary this for your computer or your Arduino

  final static int NUM_ACCEL_DIMS = 3;

  final static int RESP_ERROR_UNKNOWN = 499;
  final static int RESP_ERROR_BAD_PACKET = 500;
  final static int RESP_ERROR_BAD_NOBLE_START = 501;
  final static int RESP_ERROR_ALREADY_CONNECTED = 408;
  final static int RESP_ERROR_COMMAND_NOT_RECOGNIZED = 406;
  final static int RESP_ERROR_DEVICE_NOT_FOUND = 405;
  final static int RESP_ERROR_NO_OPEN_BLE_DEVICE = 400;
  final static int RESP_ERROR_UNABLE_TO_CONNECT = 402;
  final static int RESP_ERROR_UNABLE_TO_DISCONNECT = 401;
  final static int RESP_ERROR_SCAN_ALREADY_SCANNING = 409;
  final static int RESP_ERROR_SCAN_NONE_FOUND = 407;
  final static int RESP_ERROR_SCAN_NO_SCAN_TO_STOP = 410;
  final static int RESP_ERROR_SCAN_COULD_NOT_START = 412;
  final static int RESP_ERROR_SCAN_COULD_NOT_STOP = 411;
  final static int RESP_GANGLION_FOUND = 201;
  final static int RESP_SUCCESS = 200;
  final static int RESP_SUCCESS_DATA_ACCEL = 202;
  final static int RESP_SUCCESS_DATA_IMPEDANCE = 203;
  final static int RESP_SUCCESS_DATA_SAMPLE = 204;
  final static int RESP_STATUS_CONNECTED = 300;
  final static int RESP_STATUS_DISCONNECTED = 301;
  final static int RESP_STATUS_SCANNING = 302;
  final static int RESP_STATUS_NOT_SCANNING = 303;

  private int state = STATE_NOCOM;
  int prevState_millis = 0; // Used for calculating connect time out

  private int nEEGValuesPerPacket = NCHAN_GANGLION; // Defined by the data format sent by openBCI boards
  private int nAuxValuesPerPacket = NUM_ACCEL_DIMS; // Defined by the arduino code

  private int tcpGanglionPort = 10996;
  private String tcpGanglionIP = "127.0.0.1";
  private String tcpGanglionFull = tcpGanglionIP + ":" + tcpGanglionPort;
  private boolean tcpClientActive = false;
  private int tcpTimeout = 1000;

  private final float fs_Hz = 200.0f;  //sample rate used by OpenBCI Ganglion board... set by its Arduino code
  private final float MCP3912_Vref = 1.2f;  // reference voltage for ADC in MCP3912 set in hardware
  private float MCP3912_gain = 1.0f;  //assumed gain setting for MCP3912.  NEEDS TO BE ADJUSTABLE JM
  private float scale_fac_uVolts_per_count = (MCP3912_Vref * 1000000.f) / (8388607.0f * MCP3912_gain * 1.5f * 51.0f); //MCP3912 datasheet page 34. Gain of InAmp = 80
  // private float scale_fac_accel_G_per_count = 0.032;
  private float scale_fac_accel_G_per_count = 0.016f;
  // private final float scale_fac_accel_G_per_count = 0.002 / ((float)pow(2,4));  //assume set to +/4G, so 2 mG per digit (datasheet). Account for 4 bits unused
  // private final float leadOffDrive_amps = 6.0e-9;  //6 nA, set by its Arduino code

  private int bleErrorCounter = 0;
  private int prevSampleIndex = 0;

  private DataPacket_ADS1299 dataPacket;

  public Client tcpClient;
  private boolean portIsOpen = false;
  private boolean connected = false;

  public int numberOfDevices = 0;
  public int maxNumberOfDevices = 10;
  public String[] deviceList = new String[0];
  public boolean deviceListUpdated = false;
  private boolean hubRunning = false;
  public char[] tcpBuffer = new char[1024];
  public int tcpBufferPositon = 0;

  private boolean waitingForResponse = false;
  private boolean nodeProcessHandshakeComplete = false;
  public boolean shouldStartNodeApp = false;
  private boolean checkingImpedance = false;
  private boolean accelModeActive = false;
  private boolean newAccelData = false;
  private int[] accelArray = new int[NUM_ACCEL_DIMS];

  public boolean impedanceUpdated = false;
  public int[] impedanceArray = new int[NCHAN_GANGLION + 1];

  // Getters
  public float get_fs_Hz() { return fs_Hz; }
  public boolean isPortOpen() { return portIsOpen; }
  public float get_scale_fac_uVolts_per_count() { return scale_fac_uVolts_per_count; }
  public float get_scale_fac_accel_G_per_count() { return scale_fac_accel_G_per_count; }
  public boolean isHubRunning() { return hubRunning; }
  public boolean isCheckingImpedance() { return checkingImpedance; }
  public boolean isAccelModeActive() { return accelModeActive; }

  private PApplet mainApplet;

  //constructors
  OpenBCI_Ganglion() {};  //only use this if you simply want access to some of the constants
  OpenBCI_Ganglion(PApplet applet) {
    mainApplet = applet;

    // Able to start tcpClient connection?
    startTCPClient(mainApplet);
    // if (getStatus()) {
    //   println("Able to start tcpClient connection -- YES");
    //   hubRunning = true;
    // } else {
    //   println("Able to start tcpClient connection -- NO");
    // }

    // if (getStatus()) {
    //   println("Able to send status message, now waiting for response.");
    // } else {
    //   // We should try to start the node process because we were not able to
    //   //  establish a connection with the node process.
    //   println("Failure: Not able to send status message. Trying to start tcpConnection.");
    //   startTCPClient(applet);
    //   if (getStatus()) {
    //     println("Connection established with node server.");
    //   } else {
    //     println("Connection failed to establish with node server. Recommend trying to launch application from data dir.");
    //     shouldStartNodeApp = true;
    //   }
    // }

    // For storing data into
    dataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    for(int i = 0; i < nEEGValuesPerPacket; i++) {
      dataPacket.values[i] = 0;
    }
    for(int i = 0; i < nAuxValuesPerPacket; i++){
      dataPacket.auxValues[i] = 0;
    }
  }

  /**
   * @descirpiton Used to `try` and start the tcpClient
   * @param applet {PApplet} - The main applet.
   * @return {boolean} - True if able to start.
   */
  public boolean startTCPClient(PApplet applet) {
    try {
      tcpClient = new Client(applet, tcpGanglionIP, tcpGanglionPort);
      return true;
    } catch (Exception e) {
      println("startTCPClient: ConnectException: " + e);
      return false;
    }
  }


  /**
   * Sends a status message to the node process.
   */
  public boolean getStatus() {
    try {
      safeTCPWrite(TCP_CMD_STATUS + TCP_STOP);
      waitingForResponse = true;
      return true;
    } catch (NullPointerException E) {
      // The tcp client is not initalized, try now

      return false;
    }
  }

  public void setHubIsRunning(boolean isRunning) {
    hubRunning = isRunning;
  }

  // Return true if the display needs to be updated for the BLE list
  public void parseMessage(String msg) {
    // println(msg);
    String[] list = split(msg, ',');
    switch (list[0].charAt(0)) {
      case 'c': // Connect
        processConnect(msg);
        break;
      case 'a': // Accel
        processAccel(msg);
        break;
      case 'd': // Disconnect
        processDisconnect(msg);
        break;
      case 'i': // Impedance
        processImpedance(msg);
        break;
      case 't': // Data
        processData(msg);
        break;
      case 'e': // Error
        println("OpenBCI_Ganglion: parseMessage: error: " + list[2]);
        break;
      case 's': // Scan
        processScan(msg);
        break;
      case 'l':
        println("OpenBCI_Ganglion: Log: " + list[1]);
        break;
      case 'q':
        processStatus(msg);
        break;
      default:
        println("OpenBCI_Ganglion: parseMessage: default: " + msg);
        break;
    }
  }

  private void processAccel(String msg) {
    String[] list = split(msg, ',');
    if (Integer.parseInt(list[1]) == RESP_SUCCESS_DATA_ACCEL) {
      for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
        accelArray[i] = Integer.parseInt(list[i + 2]);
      }
      newAccelData = true;
    }
  }

  private void processConnect(String msg) {
    String[] list = split(msg, ',');
    if (isSuccessCode(Integer.parseInt(list[1]))) {
      println("OpenBCI_Ganglion: parseMessage: connect: success!");
      output("OpenBCI_Ganglion: The GUI is done intializing. Click outside of the control panel to interact with the GUI.");
      systemMode = 10;
      connected = true;
      controlPanel.close();
    } else {
      println("OpenBCI_Ganglion: parseMessage: connect: failure!");
      haltSystem();
      initSystemButton.setString("START SYSTEM");
      controlPanel.open();
      abandonInit = true;
      output("Unable to connect to Ganglion! Please ensure board is powered on and in range!");
      connected = false;
    }
  }

  private void processData(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    if (eegDataSource == DATASOURCE_GANGLION && systemMode == 10 && isRunning) {
      if (Integer.parseInt(list[1]) == RESP_SUCCESS_DATA_SAMPLE) {
        // Sample number stuff
        dataPacket.sampleIndex = PApplet.parseInt(Integer.parseInt(list[2]));
        if ((dataPacket.sampleIndex - prevSampleIndex) != 1) {
          if(dataPacket.sampleIndex != 0){  // if we rolled over, don't count as error
            bleErrorCounter++;

            werePacketsDroppedGang = true; //set this true to activate packet duplication in serialEvent
            if(dataPacket.sampleIndex < prevSampleIndex){   //handle the situation in which the index jumps from 250s past 255, and back to 0
              numPacketsDroppedGang = (dataPacket.sampleIndex+200) - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
            } else {
              numPacketsDroppedGang = dataPacket.sampleIndex - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
            }
            println("OpenBCI_Ganglion: apparent sampleIndex jump from Serial data: " + prevSampleIndex + " to  " + dataPacket.sampleIndex + ".  Keeping packet. (" + bleErrorCounter + ")");
            println("numPacketsDropped = " + numPacketsDropped);
          }
        }
        prevSampleIndex = dataPacket.sampleIndex;

        // Channel data storage
        for (int i = 0; i < NCHAN_GANGLION; i++) {
          dataPacket.values[i] = Integer.parseInt(list[3 + i]);
        }
        if (newAccelData) {
          newAccelData = false;
          for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
            dataPacket.auxValues[i] = accelArray[i];
            dataPacket.rawAuxValues[i][0] = PApplet.parseByte(accelArray[i]);
          }
        }
        getRawValues(dataPacket);
        // println(binary(dataPacket.values[0], 24) + '\n' + binary(dataPacket.rawValues[0][0], 8) + binary(dataPacket.rawValues[0][1], 8) + binary(dataPacket.rawValues[0][2], 8) + '\n');
        curDataPacketInd = (curDataPacketInd+1) % dataPacketBuff.length; // This is also used to let the rest of the code that it may be time to do something

        ganglion.copyDataPacketTo(dataPacketBuff[curDataPacketInd]);  // Resets isNewDataPacketAvailable to false

        // KILL SPIKES!!!
        if(werePacketsDroppedGang){
          // println("Packets Dropped ... doing some stuff...");
          for(int i = numPacketsDroppedGang; i > 0; i--){
            int tempDataPacketInd = curDataPacketInd - i; //
            if(tempDataPacketInd >= 0 && tempDataPacketInd < dataPacketBuff.length){
              // println("i = " + i);
              ganglion.copyDataPacketTo(dataPacketBuff[tempDataPacketInd]);
            } else {
              ganglion.copyDataPacketTo(dataPacketBuff[tempDataPacketInd+200]);
            }
            //put the last stored packet in # of packets dropped after that packet
          }

          //reset werePacketsDropped & numPacketsDropped
          werePacketsDroppedGang = false;
          numPacketsDroppedGang = 0;
        }

        switch (outputDataSource) {
          case OUTPUT_SOURCE_ODF:
            fileoutput_odf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd], ganglion.get_scale_fac_uVolts_per_count(), get_scale_fac_accel_G_per_count());
            break;
          case OUTPUT_SOURCE_BDF:
            // curBDFDataPacketInd = curDataPacketInd;
            // thread("writeRawData_dataPacket_bdf");
            fileoutput_bdf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd]);
            break;
          case OUTPUT_SOURCE_NONE:
          default:
            // Do nothing...
            break;
        }
        newPacketCounter++;
      } else {
        bleErrorCounter++;
        println("OpenBCI_Ganglion: parseMessage: data: bad");
      }
    }
  }

  private void handleError(int code, String msg) {
    output("Code " + code + "Error: " + msg);
    println("Code " + code + "Error: " + msg);
  }

  private void processDisconnect(String msg) {
    if (!waitingForResponse) {
      haltSystem();
      initSystemButton.setString("START SYSTEM");
      controlPanel.open();
      output("Dang! Lost connection to Ganglion. Please move closer or get a new battery!");
    } else {
      waitingForResponse = false;
    }
  }

  private void processImpedance(String msg) {
    String[] list = split(msg, ',');
    if (Integer.parseInt(list[1]) == RESP_SUCCESS_DATA_IMPEDANCE) {
      int channel = Integer.parseInt(list[2]);
      if (channel < 5) {
        int value = Integer.parseInt(list[3]);
        impedanceArray[channel] = value;
        if (channel == 0) {
          impedanceUpdated = true;
          println("Impedance for channel reference is " + value + " ohms.");
        } else {
          println("? for channel " + channel + " is " + value + " ohms.");
        }
      }
    }
  }
  
  private void processStatus(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    if (waitingForResponse) {
      waitingForResponse = false;
      println("Node process up!");
    }
    if (code == RESP_ERROR_BAD_NOBLE_START) {
      println("OpenBCI_Ganglion: processStatus: Problem in the Hub");
      output("Problem starting Ganglion Hub. Please make sure compatible USB is configured, then restart this GUI.");
    } else {    
      println("OpenBCI_Ganglion: processStatus: Started Successfully");
    }
  }

  private void processScan(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch(code) {
      case RESP_GANGLION_FOUND:
        // Sent every time a new ganglion device is found
        if (searchDeviceAdd(list[2])) {
          deviceListUpdated = true;
        }
        break;
      case RESP_ERROR_SCAN_ALREADY_SCANNING:
        // Sent when a start send command is sent and the module is already
        //  scanning.
        handleError(code, list[2]);
        break;
      case RESP_SUCCESS:
        // Sent when either a scan was stopped or started Successfully
        String action = list[2];
        switch (action) {
          case TCP_ACTION_START:
            break;
          case TCP_ACTION_STOP:
            break;
        }
        break;
      case RESP_ERROR_SCAN_COULD_NOT_START:
        // Sent when err on search start
        handleError(code, list[2]);
        break;
      case RESP_ERROR_SCAN_COULD_NOT_STOP:
        // Send when err on search stop
        handleError(code, list[2]);
        break;
      case RESP_STATUS_SCANNING:
        // Sent when after status action sent to node and module is searching
        break;
      case RESP_STATUS_NOT_SCANNING:
        // Sent when after status action sent to node and module is NOT searching
        break;
      case RESP_ERROR_SCAN_NO_SCAN_TO_STOP:
        // Sent when a 'stop' action is sent to node and there is no scan to stop.
        handleError(code, list[2]);
        break;
      case RESP_ERROR_UNKNOWN:
      default:
        handleError(code, list[2]);
        break;
    }
  }

  public void writeRawData_dataPacket_bdf() {
    fileoutput_bdf.writeRawData_dataPacket(dataPacketBuff[curBDFDataPacketInd]);
  }

  public int copyDataPacketTo(DataPacket_ADS1299 target) {
    return dataPacket.copyTo(target);
  }

  private void getRawValues(DataPacket_ADS1299 packet) {
    for (int i=0; i < nchan; i++) {
      int val = packet.values[i];
      //println(binary(val, 24));
      byte rawValue[] = new byte[3];
      // Breakdown values into
      rawValue[2] = PApplet.parseByte(val & 0xFF);
      //println("rawValue[2] " + binary(rawValue[2], 8));
      rawValue[1] = PApplet.parseByte((val & (0xFF << 8)) >> 8);
      //println("rawValue[1] " + binary(rawValue[1], 8));
      rawValue[0] = PApplet.parseByte((val & (0xFF << 16)) >> 16);
      //println("rawValue[0] " + binary(rawValue[0], 8));
      // Store to the target raw values
      packet.rawValues[i] = rawValue;
      //println();
    }
  }

  // TODO: Figure out how to ping the server at localhost listening on port 10996
  // /**
  //  * Used to ping the local hub tcp server and check it's status.
  //  */
  // public boolean pingHub() {
  //   boolean pingStat;
  //
  //   try {
  //     println("GanglionSync: pingHub: trying... ");
  //     pingStat = InetAddress.getByName("127.0.0.1:10996").isReachable(tcpTimeout);
  //     print("GanglionSync: pingHub: ");
  //     println(pingStat);
  //     return pingStat;
  //   }
  //   catch(Exception E){
  //     E.printStackTrace();
  //     return false;
  //   }
  // }

  public boolean isSuccessCode(int c) {
    return c == RESP_SUCCESS;
  }

  // SCANNING/SEARHING FOR DEVICES

  public void searchDeviceStart() {
    deviceList = null;
    numberOfDevices = 0;
    safeTCPWrite(TCP_CMD_SCAN + ',' + TCP_ACTION_START + TCP_STOP);
  }

  public void searchDeviceStop() {
    safeTCPWrite(TCP_CMD_SCAN + ',' + TCP_ACTION_STOP + TCP_STOP);
  }

  public boolean searchDeviceAdd(String ganglionLocalName) {
    if (numberOfDevices == 0) {
      numberOfDevices++;
      deviceList = new String[numberOfDevices];
      deviceList[0] = ganglionLocalName;
      return true;
    } else {
      boolean willAddToDeviceList = true;
      for (int i = 0; i < numberOfDevices; i++) {
        if (ganglionLocalName.equals(deviceList[i])) {
          willAddToDeviceList = false;
          break;
        }
      }
      if (willAddToDeviceList) {
        numberOfDevices++;
        String[] tempList = new String[numberOfDevices];
        arrayCopy(deviceList, tempList);
        tempList[numberOfDevices - 1] = ganglionLocalName;
        deviceList = tempList;
        return true;
      }
    }
    return false;
  }

  // CONNECTION
  public void connectBLE(String id) {
    safeTCPWrite(TCP_CMD_CONNECT + "," + id + TCP_STOP);
  }

  public void disconnectBLE() {
    waitingForResponse = true;
    safeTCPWrite(TCP_CMD_DISCONNECT + TCP_STOP);
  }

  public void updateSyncState() {
    //has it been 3000 milliseconds since we initiated the serial port? We want to make sure we wait for the OpenBCI board to finish its setup()
    if ((millis() - prevState_millis > COM_INIT_MSEC) && (prevState_millis != 0) && (state == openBCI.STATE_COMINIT) ) {
      // We are synced and ready to go!
      state = STATE_SYNCWITHHARDWARE;
      println("OpenBCI_Ganglion: Sending reset command");
      // serial_openBCI.write('v');
    }
  }

  /**
   * @description Sends a start streaming command to the Ganglion Node module.
   */
  public void startDataTransfer(){
    changeState(STATE_NORMAL);  // make sure it's now interpretting as binary
    println("OpenBCI_Ganglion: startDataTransfer(): sending \'" + command_startBinary);
    safeTCPWrite(TCP_CMD_COMMAND + "," + command_startBinary + TCP_STOP);
  }

  /**
   * @description Sends a stop streaming command to the Ganglion Node module.
   */
  public void stopDataTransfer() {
    changeState(STATE_STOPPED);  // make sure it's now interpretting as binary
    println("OpenBCI_Ganglion: stopDataTransfer(): sending \'" + command_stop);
    safeTCPWrite(TCP_CMD_COMMAND + "," + command_stop + TCP_STOP);
  }

  /**
   * @description Write to TCP server
   * @params out {String} - The string message to write to the server.
   * @returns {boolean} - True if able to write, false otherwise.
   */
  public boolean safeTCPWrite(String out) {
    try {
      tcpClient.write(out);
      return true;
    } catch (Exception e) {
      println("Error: Attempted to TCP write with no server connection initialized");
      return false;
    }
    // return false;
    // if (nodeProcessHandshakeComplete) {
    //   try {
    //     tcpClient.write(out);
    //     return true;
    //   } catch (NullPointerException e) {
    //     println("Error: Attempted to TCP write with no server connection initialized");
    //     return false;
    //   }
    // } else {
    //   println("Waiting on node handshake!");
    //   return false;
    // }
  }

  private void printGanglion(String msg) {
    print("OpenBCI_Ganglion: "); println(msg);
  }

  public int changeState(int newState) {
    state = newState;
    prevState_millis = millis();
    return 0;
  }

  // Channel setting
  //activate or deactivate an EEG channel...channel counting is zero through nchan-1
  public void changeChannelState(int Ichan, boolean activate) {
    if (connected) {
      if ((Ichan >= 0)) {
        if (activate) {
          println("OpenBCI_Ganglion: changeChannelState(): activate: sending " + command_activate_channel[Ichan]);
          safeTCPWrite(TCP_CMD_COMMAND + "," + command_activate_channel[Ichan] + TCP_STOP);
          w_timeSeries.hsc.powerUpChannel(Ichan);
        } else {
          println("OpenBCI_Ganglion: changeChannelState(): deactivate: sending " + command_deactivate_channel[Ichan]);
          safeTCPWrite(TCP_CMD_COMMAND + "," + command_deactivate_channel[Ichan] + TCP_STOP);
          w_timeSeries.hsc.powerDownChannel(Ichan);
        }
      }
    }
  }

  /**
   * Used to start accel data mode. Accel arrays will arrive asynchronously!
   */
  public void accelStart() {
    println("OpenBCI_Ganglion: accell: START");
    safeTCPWrite(TCP_CMD_ACCEL + "," + TCP_ACTION_START + TCP_STOP);
    accelModeActive = true;
  }

  /**
   * Used to stop accel data mode. Some accel arrays may arrive after stop command
   *  was sent by this function.
   */
  public void accelStop() {
    println("OpenBCI_Ganglion: accel: STOP");
    safeTCPWrite(TCP_CMD_ACCEL + "," + TCP_ACTION_STOP + TCP_STOP);
    accelModeActive = false;
  }

  /**
   * Used to start impedance testing. Impedances will arrive asynchronously!
   */
  public void impedanceStart() {
    println("OpenBCI_Ganglion: impedance: START");
    safeTCPWrite(TCP_CMD_IMPEDANCE + "," + TCP_ACTION_START + TCP_STOP);
    checkingImpedance = true;
  }

  /**
   * Used to stop impedance testing. Some impedances may arrive after stop command
   *  was sent by this function.
   */
  public void impedanceStop() {
    println("OpenBCI_Ganglion: impedance: STOP");
    safeTCPWrite(TCP_CMD_IMPEDANCE + "," + TCP_ACTION_STOP + TCP_STOP);
    checkingImpedance = false;
  }

  /**
   * Puts the ganglion in bootloader mode.
   */
  public void enterBootloaderMode() {
    println("OpenBCI_Ganglion: Entering Bootloader Mode");
    safeTCPWrite(TCP_CMD_COMMAND + "," + GANGLION_BOOTLOADER_MODE + TCP_STOP);
    delay(500);
    disconnectBLE();
    haltSystem();
    initSystemButton.setString("START SYSTEM");
    controlPanel.open();
    output("Ganglion now in bootloader mode! Enjoy!");
  }
};
//////////////////////////////////////////////////////////////////////////
//
//    Hardware Settings Controller
//    - this is the user interface for allowing you to control the hardware settings of the 32bit Board & 16chan Setup (32bit + Daisy)
//
//    Written by: Conor Russomanno (Oct. 2016) ... adapted from ChannelController.pde of GUI V1 ... it's a little bit simpler now :|
//    Based on some original GUI code by: Chip Audette 2013/2014
//
//////////////////////////////////////////////////////////////////////////


//these arrays of channel values need to be global so that they don't reset on screen resize, when GUI reinitializes (there's definitely a more efficient way to do this...)
int numSettingsPerChannel = 6; //each channel has 6 different settings
char[][] channelSettingValues = new char [nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
char[][] impedanceCheckValues = new char [nchan][2];

public void updateChannelArrays(int _nchan) {
  channelSettingValues = new char [_nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
  impedanceCheckValues = new char [_nchan][2];
}

//activateChannel: Ichan is [0 nchan-1] (aka zero referenced)
public void activateChannel(int Ichan) {
  println(appName + ": activating channel " + (Ichan+1));
  if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
    if (openBCI.isSerialPortOpen()) {
      verbosePrint("**");
      openBCI.changeChannelState(Ichan, true); //activate
    }
  } else if (eegDataSource == DATASOURCE_GANGLION) {
    // println("activating channel on ganglion");
    ganglion.changeChannelState(Ichan, true);
  }
  if (Ichan < nchan) {
    channelSettingValues[Ichan][0] = '0';
    // gui.cc.update();
  }
}
public void deactivateChannel(int Ichan) {
  println(appName + ": deactivating channel " + (Ichan+1));
  if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
    if (openBCI.isSerialPortOpen()) {
      verbosePrint("**");
      openBCI.changeChannelState(Ichan, false); //de-activate
    }
  } else if (eegDataSource == DATASOURCE_GANGLION) {
    // println("deactivating channel on ganglion");
    ganglion.changeChannelState(Ichan, false);
  }
  if (Ichan < nchan) {
    channelSettingValues[Ichan][0] = '1';
    // gui.cc.update();
  }
}

//Ichan is zero referenced (not one referenced)
public boolean isChannelActive(int Ichan) {
  boolean return_val = false;
  if (channelSettingValues[Ichan][0] == '1') {
    return_val = false;
  } else {
    return_val = true;
  }
  return return_val;
}

class HardwareSettingsController{

  boolean isVisible = false;

  int x, y, w, h;

  int numSettingsPerChannel = 6; //each channel has 6 different settings
  char[][] channelSettingValues = new char [nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
  char[][] impedanceCheckValues = new char [nchan][2];

  int spaceBetweenButtons = 5; //space between buttons

  // [Number of Channels] x 6 array of buttons for channel settings
  Button[][] channelSettingButtons = new Button [nchan][numSettingsPerChannel];  // [channel#][Button#]

  // Array for storing SRB2 history settings of channels prior to shutting off .. so you can return to previous state when reactivating channel
  char[] previousSRB2 = new char [nchan];
  // Array for storing SRB2 history settings of channels prior to shutting off .. so you can return to previous state when reactivating channel
  char[] previousBIAS = new char [nchan];

  //maximum different values for the different settings (Power Down, Gain, Input Type, BIAS, SRB2, SRB1) of
  //refer to page 44 of ADS1299 Datasheet: http://www.ti.com/lit/ds/symlink/ads1299.pdf
  char[] maxValuesPerSetting = {
    '1', // Power Down :: (0)ON, (1)OFF
    '6', // Gain :: (0) x1, (1) x2, (2) x4, (3) x6, (4) x8, (5) x12, (6) x24 ... default
    '7', // Channel Input :: (0)Normal Electrode Input, (1)Input Shorted, (2)Used in conjunction with BIAS_MEAS, (3)MVDD for supply measurement, (4)Temperature Sensor, (5)Test Signal, (6)BIAS_DRP ... positive electrode is driver, (7)BIAS_DRN ... negative electrode is driver
    '1', // BIAS :: (0) Yes, (1) No
    '1', // SRB2 :: (0) Open, (1) Closed
    '1'
  }; // SRB1 :: (0) Yes, (1) No ... this setting affects all channels ... either all on or all off

  //variables used for channel write timing in writeChannelSettings()
  int channelToWrite = -1;

  //variables use for imp write timing with writeImpedanceSettings()
  int impChannelToWrite = -1;

  boolean rewriteChannelWhenDoneWriting = false;
  int channelToWriteWhenDoneWriting = 0;

  boolean rewriteImpedanceWhenDoneWriting = false;
  int impChannelToWriteWhenDoneWriting = 0;
  char final_pORn = '0';
  char final_onORoff = '0';

  HardwareSettingsController(int _x, int _y, int _w, int _h, int _channelBarHeight){
    x = _x;
    y = _y;
    w = _w;
    h = _h;

    createChannelSettingButtons(_channelBarHeight);

  }

  public void update(){
    //make false to check again below
    // for (int i = 0; i < nchan; i++) {
    //   drawImpedanceValues[i] = false;
    // }

    for (int i = 0; i < nchan; i++) { //for every channel
      //update buttons based on channelSettingValues[i][j]
      for (int j = 0; j < numSettingsPerChannel; j++) {
        switch(j) {  //what setting are we looking at
          case 0: //on/off ??
            // if (channelSettingValues[i][j] == '0') channelSettingButtons[i][0].setColorNotPressed(channelColors[i%8]);// power down == false, set color to vibrant
            if (channelSettingValues[i][j] == '0') w_timeSeries.channelBars[i].onOffButton.setColorNotPressed(channelColors[i%8]);// power down == false, set color to vibrant
            if (channelSettingValues[i][j] == '1') w_timeSeries.channelBars[i].onOffButton.setColorNotPressed(75); // power down == true, set color to dark gray, indicating power down
            break;

          case 1: //GAIN ??
            if (channelSettingValues[i][j] == '0') channelSettingButtons[i][1].setString("x1");
            if (channelSettingValues[i][j] == '1') channelSettingButtons[i][1].setString("x2");
            if (channelSettingValues[i][j] == '2') channelSettingButtons[i][1].setString("x4");
            if (channelSettingValues[i][j] == '3') channelSettingButtons[i][1].setString("x6");
            if (channelSettingValues[i][j] == '4') channelSettingButtons[i][1].setString("x8");
            if (channelSettingValues[i][j] == '5') channelSettingButtons[i][1].setString("x12");
            if (channelSettingValues[i][j] == '6') channelSettingButtons[i][1].setString("x24");
            break;
          case 2: //input type ??
            if (channelSettingValues[i][j] == '0') channelSettingButtons[i][2].setString("Normal");
            if (channelSettingValues[i][j] == '1') channelSettingButtons[i][2].setString("Shorted");
            if (channelSettingValues[i][j] == '2') channelSettingButtons[i][2].setString("BIAS_MEAS");
            if (channelSettingValues[i][j] == '3') channelSettingButtons[i][2].setString("MVDD");
            if (channelSettingValues[i][j] == '4') channelSettingButtons[i][2].setString("Temp.");
            if (channelSettingValues[i][j] == '5') channelSettingButtons[i][2].setString("Test");
            if (channelSettingValues[i][j] == '6') channelSettingButtons[i][2].setString("BIAS_DRP");
            if (channelSettingValues[i][j] == '7') channelSettingButtons[i][2].setString("BIAS_DRN");
            break;
          case 3: //BIAS ??
            if (channelSettingValues[i][j] == '0') channelSettingButtons[i][3].setString("Don't Include");
            if (channelSettingValues[i][j] == '1') channelSettingButtons[i][3].setString("Include");
            break;
          case 4: // SRB2 ??
            if (channelSettingValues[i][j] == '0') channelSettingButtons[i][4].setString("Off");
            if (channelSettingValues[i][j] == '1') channelSettingButtons[i][4].setString("On");
            break;
          case 5: // SRB1 ??
            if (channelSettingValues[i][j] == '0') channelSettingButtons[i][5].setString("No");
            if (channelSettingValues[i][j] == '1') channelSettingButtons[i][5].setString("Yes");
            break;
        }
      }

      // needs to be updated to work with single imp button ...
      // for (int k = 0; k < 2; k++) {
      //   switch(k) {
      //     case 0: // P Imp Buttons
      //       if (impedanceCheckValues[i][k] == '0') {
      //         impedanceCheckButtons[i][0].setColorNotPressed(color(75));
      //         impedanceCheckButtons[i][0].setString("");
      //       }
      //       if (impedanceCheckValues[i][k] == '1') {
      //         impedanceCheckButtons[i][0].setColorNotPressed(isSelected_color);
      //         impedanceCheckButtons[i][0].setString("");
      //         if (showFullController) {
      //           drawImpedanceValues[i] = false;
      //         } else {
      //           drawImpedanceValues[i] = true;
      //         }
      //       }
      //       break;
      //     case 1: // N Imp Buttons
      //       if (impedanceCheckValues[i][k] == '0') {
      //         impedanceCheckButtons[i][1].setColorNotPressed(color(75));
      //         impedanceCheckButtons[i][1].setString("");
      //       }
      //       if (impedanceCheckValues[i][k] == '1') {
      //         impedanceCheckButtons[i][1].setColorNotPressed(isSelected_color);
      //         impedanceCheckButtons[i][1].setString("");
      //         if (showFullController) {
      //           drawImpedanceValues[i] = false;
      //         } else {
      //           drawImpedanceValues[i] = true;
      //         }
      //       }
      //       break;
      //   }
      // }
    }
    //then reset to 1

    //
    if (openBCI.get_isWritingChannel()) {
      openBCI.writeChannelSettings(channelToWrite,channelSettingValues);
    }

    if (rewriteChannelWhenDoneWriting == true && openBCI.get_isWritingChannel() == false) {
      initChannelWrite(channelToWriteWhenDoneWriting);
      rewriteChannelWhenDoneWriting = false;
    }

    if (openBCI.get_isWritingImp()) {
      openBCI.writeImpedanceSettings(impChannelToWrite,impedanceCheckValues);
    }

    if (rewriteImpedanceWhenDoneWriting == true && openBCI.get_isWritingImp() == false) {
      initImpWrite(impChannelToWriteWhenDoneWriting, final_pORn, final_onORoff);
      rewriteImpedanceWhenDoneWriting = false;
    }
  }

  public void draw(){

        pushStyle();

        if (isVisible) {

          //background
          noStroke();
          fill(0, 0, 0, 100);
          rect(x, y, w, h);

          // [numChan] x 5 ... all channel setting buttons (other than on/off)
          for (int i = 0; i < nchan; i++) {
            for (int j = 1; j < 6; j++) {
              channelSettingButtons[i][j].draw();
            }
          }

          //draw column headers for channel settings behind EEG graph
          // fill(bgColor);
          // text("PGA Gain", x2 + (w2/10)*1, y1 - 12);
          // text("Input Type", x2 + (w2/10)*3, y1 - 12);
          // text("  Bias ", x2 + (w2/10)*5, y1 - 12);
          // text("SRB2", x2 + (w2/10)*7, y1 - 12);
          // text("SRB1", x2 + (w2/10)*9, y1 - 12);

          //if mode is not from OpenBCI, draw a dark overlay to indicate that you cannot edit these settings
          if (eegDataSource != DATASOURCE_NORMAL_W_AUX) {
            fill(0, 0, 0, 200);
            noStroke();
            rect(x-2, y, w+1, h);
            fill(255);
            textAlign(CENTER,CENTER);
            textFont(h1,18);
            text("DATA SOURCE (LIVE) only", x + (w/2), y + (h/2));
          }
        }

        // for (int i = 0; i < nchan; i++) {
        //   if (drawImpedanceValues[i] == true) {
        //     gui.impValuesMontage[i].draw();  //impedance values on montage plot
        //   }
        // }

        popStyle();
  }

  public void loadDefaultChannelSettings() {
    verbosePrint("ChannelController: loading default channel settings to GUI's channel controller...");
    for (int i = 0; i < nchan; i++) {
      verbosePrint("chan: " + i + " ");
      for (int j = 0; j < numSettingsPerChannel; j++) { //channel setting values
        channelSettingValues[i][j] = PApplet.parseChar(openBCI.get_defaultChannelSettings().toCharArray()[j]); //parse defaultChannelSettings string created in the OpenBCI_ADS1299 class
        if (j == numSettingsPerChannel - 1) {
          println(PApplet.parseChar(openBCI.get_defaultChannelSettings().toCharArray()[j]));
        } else {
          print(PApplet.parseChar(openBCI.get_defaultChannelSettings().toCharArray()[j]) + ",");
        }
      }
      for (int k = 0; k < 2; k++) { //impedance setting values
        impedanceCheckValues[i][k] = '0';
      }
    }
    verbosePrint("made it!");
    update(); //update 1 time to refresh button values based on new loaded settings
  }

  public void updateChannelArrays(int _nchan) {
    channelSettingValues = new char [_nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
    impedanceCheckValues = new char [_nchan][2];
  }

  //activateChannel: Ichan is [0 nchan-1] (aka zero referenced)
  public void activateChannel(int Ichan) {
    println(appName + ": activating channel " + (Ichan+1));
    if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
      if (openBCI.isSerialPortOpen()) {
        verbosePrint("**");
        openBCI.changeChannelState(Ichan, true); //activate
      }
    } else if (eegDataSource == DATASOURCE_GANGLION) {
      // println("activating channel on ganglion");
      ganglion.changeChannelState(Ichan, true);
    }
    if (Ichan < nchan) {
      channelSettingValues[Ichan][0] = '0';
      w_timeSeries.hsc.update(); //previously gui.cc.update();
    }
  }

  public void deactivateChannel(int Ichan) {
    println(appName + ": deactivating channel " + (Ichan+1));
    if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
      if (openBCI.isSerialPortOpen()) {
        verbosePrint("**");
        openBCI.changeChannelState(Ichan, false); //de-activate
      }
    } else if (eegDataSource == DATASOURCE_GANGLION) {
      // println("deactivating channel on ganglion");
      ganglion.changeChannelState(Ichan, false);
    }
    if (Ichan < nchan) {
      channelSettingValues[Ichan][0] = '1';
      w_timeSeries.hsc.update();
    }
  }

  //Ichan is zero referenced (not one referenced)
  public boolean isChannelActive(int Ichan) {
    boolean return_val = false;
    if (channelSettingValues[Ichan][0] == '1') {
      return_val = false;
    } else {
      return_val = true;
    }
    return return_val;
  }

  public void powerDownChannel(int _numChannel) {
    verbosePrint("Powering down channel " + str(PApplet.parseInt(_numChannel) + PApplet.parseInt(1)));
    //save SRB2 and BIAS settings in 2D history array (to turn back on when channel is reactivated)
    previousBIAS[_numChannel] = channelSettingValues[_numChannel][3];
    previousSRB2[_numChannel] = channelSettingValues[_numChannel][4];
    channelSettingValues[_numChannel][3] = '0'; //make sure to disconnect from BIAS
    channelSettingValues[_numChannel][4] = '0'; //make sure to disconnect from SRB2

    channelSettingValues[_numChannel][0] = '1'; //update powerUp/powerDown value of 2D array
    verbosePrint("Command: " + command_deactivate_channel[_numChannel]);
    openBCI.deactivateChannel(_numChannel);  //assumes numChannel counts from zero (not one)...handles regular and daisy channels
  }

  public void powerUpChannel(int _numChannel) {
    verbosePrint("Powering up channel " + str(PApplet.parseInt(_numChannel) + PApplet.parseInt(1)));
    //replace SRB2 and BIAS settings with values from 2D history array
    channelSettingValues[_numChannel][3] = previousBIAS[_numChannel];
    channelSettingValues[_numChannel][4] = previousSRB2[_numChannel];

    channelSettingValues[_numChannel][0] = '0'; //update powerUp/powerDown value of 2D array
    verbosePrint("Command: " + command_activate_channel[_numChannel]);
    openBCI.activateChannel(_numChannel);  //assumes numChannel counts from zero (not one)...handles regular and daisy channels//assumes numChannel counts from zero (not one)...handles regular and daisy channels
  }

  public void initChannelWrite(int _numChannel) {
    //after clicking any button, write the new settings for that channel to OpenBCI
    if (!openBCI.get_isWritingImp()) { //make sure you aren't currently writing imp settings for a channel
      verbosePrint("Writing channel settings for channel " + str(_numChannel+1) + " to OpenBCI!");
      openBCI.initChannelWrite(_numChannel);
      channelToWrite = _numChannel;
    }
  }

  public void initImpWrite(int _numChannel, char pORn, char onORoff) {
    //after clicking any button, write the new settings for that channel to OpenBCI
    if (!openBCI.get_isWritingChannel()) { //make sure you aren't currently writing imp settings for a channel
      // if you're not currently writing a channel and not waiting to rewrite after you've finished mashing the button
      if (!openBCI.get_isWritingImp() && rewriteImpedanceWhenDoneWriting == false) {
        verbosePrint("Writing impedance check settings (" + pORn + "," + onORoff +  ") for channel " + str(_numChannel+1) + " to OpenBCI!");
        if (pORn == 'p') {
          impedanceCheckValues[_numChannel][0] = onORoff;
        }
        if (pORn == 'n') {
          impedanceCheckValues[_numChannel][1] = onORoff;
        }
        openBCI.initImpWrite(_numChannel);
        impChannelToWrite = _numChannel;
      } else { //else wait until a the current write has finished and then write again ... this is to not overwrite the wrong values while writing a channel
        verbosePrint("CONGRATULATIONS, YOU'RE MASHING BUTTONS!");
        rewriteImpedanceWhenDoneWriting = true;
        impChannelToWriteWhenDoneWriting = _numChannel;

        if (pORn == 'p') {
          final_pORn = 'p';
        }
        if (pORn == 'n') {
          final_pORn = 'n';
        }
        final_onORoff = onORoff;
      }
    }
  }

  public void createChannelSettingButtons(int _channelBarHeight) {
    //the size and space of these buttons are dependendant on the size of the screen and full ChannelController

    verbosePrint("ChannelController: createChannelSettingButtons: creating channel setting buttons...");
    int buttonW = 0;
    int buttonX = 0;
    int buttonH = 0;
    int buttonY = 0; //variables to be used for button creation below
    String buttonString = "";
    Button tempButton;

    //create all other channel setting buttons... these are only visible when the user toggles to "showFullController = true"
    // for (int i = 0; i < nchan; i++) {
    //   for (int j = 1; j < 6; j++) {
    //     buttonW = int((w2 - (spacer2*6)) / 5);
    //     buttonX = int((x2 + (spacer2 * (j))) + ((j-1) * buttonW));
    //     // buttonH = int((h2 / (nchan + 1)) - (spacer2/2));
    //     buttonY = int(y2 + (((h2-1)/(nchan+1))*(i+1)) - (buttonH/2));
    //     buttonString = "N/A";
    //     tempButton = new Button (buttonX, buttonY, buttonW, buttonH, buttonString, 14);
    //     channelSettingButtons[i][j] = tempButton;
    //   }
    // }

    for (int i = 0; i < nchan; i++) {
      for (int j = 1; j < 6; j++) {
        buttonW = PApplet.parseInt((w - (spaceBetweenButtons*6)) / 5);
        buttonX = PApplet.parseInt((x + (spaceBetweenButtons * (j))) + ((j-1) * buttonW));
        buttonH = 18;
        // buttonY = int(y + ((30)*i) + (((30)-buttonH)/2)); //timeSeries_widget.channelBarHeight
        buttonY = PApplet.parseInt(y + ((_channelBarHeight)*i) + (((_channelBarHeight)-buttonH)/2)); //timeSeries_widget.channelBarHeight
        buttonString = "N/A";
        tempButton = new Button (buttonX, buttonY, buttonW, buttonH, buttonString, 14);
        channelSettingButtons[i][j] = tempButton;
      }
    }
  }

  public void mousePressed(){
    if (isVisible) {
      for (int i = 0; i < nchan; i++) { //When [i][j] button is clicked
        for (int j = 1; j < numSettingsPerChannel; j++) {
          if (channelSettingButtons[i][j].isMouseHere()) {
            //increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
            channelSettingButtons[i][j].wasPressed = true;
            channelSettingButtons[i][j].isActive = true;
          }
        }
      }
    }
  }

  public void mouseReleased(){
    if (isVisible) {
      for (int i = 0; i < nchan; i++) { //When [i][j] button is clicked
        for (int j = 1; j < numSettingsPerChannel; j++) {
          if (channelSettingButtons[i][j].isMouseHere() && channelSettingButtons[i][j].wasPressed == true) {
            if (channelSettingValues[i][j] < maxValuesPerSetting[j]) {
              channelSettingValues[i][j]++;	//increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
            } else {
              channelSettingValues[i][j] = '0';
            }
            // if you're not currently writing a channel and not waiting to rewrite after you've finished mashing the button
            if (!openBCI.get_isWritingChannel() && rewriteChannelWhenDoneWriting == false) {
              initChannelWrite(i);//write new ADS1299 channel row values to OpenBCI
            } else { //else wait until a the current write has finished and then write again ... this is to not overwrite the wrong values while writing a channel
              verbosePrint("CONGRATULATIONS, YOU'RE MASHING BUTTONS!");
              rewriteChannelWhenDoneWriting = true;
              channelToWriteWhenDoneWriting = i;
            }
          }

          // if(!channelSettingButtons[i][j].isMouseHere()){
          channelSettingButtons[i][j].isActive = false;
          channelSettingButtons[i][j].wasPressed = false;
          // }
        }
      }
    }
  }

  public void screenResized(int _x, int _y, int _w, int _h, int _channelBarHeight){
    x = _x;
    y = _y;
    w = _w;
    h = _h;

    int buttonW = 0;
    int buttonX = 0;
    int buttonH = 0;
    int buttonY = 0; //variables to be used for button creation below
    String buttonString = "";

    for (int i = 0; i < nchan; i++) {
      for (int j = 1; j < 6; j++) {
        buttonW = PApplet.parseInt((w - (spaceBetweenButtons*6)) / 5);
        buttonX = PApplet.parseInt((x + (spaceBetweenButtons * (j))) + ((j-1) * buttonW));
        buttonH = 18;
        buttonY = PApplet.parseInt(y + ((_channelBarHeight)*i) + (((_channelBarHeight)-buttonH)/2)); //timeSeries_widget.channelBarHeight
        buttonString = "N/A";
        channelSettingButtons[i][j].but_x = buttonX;
        channelSettingButtons[i][j].but_y = buttonY;
        channelSettingButtons[i][j].but_dx = buttonW;
        channelSettingButtons[i][j].but_dy = buttonH;
      }
    }
  }

  public void toggleImpedanceCheck(int _channelNumber){

    if(channelSettingValues[_channelNumber][4] == '1'){     //is N pin being used...
      if (impedanceCheckValues[_channelNumber][1] < '1') { //if not checking/drawing impedance
        initImpWrite(_channelNumber, 'n', '1');  // turn on the impedance check for the desired channel
        println("Imp[" + _channelNumber + "] is on.");
      } else {
        initImpWrite(_channelNumber, 'n', '0'); //turn off impedance check for desired channel
        println("Imp[" + _channelNumber + "] is off.");
      }
    }

    if(channelSettingValues[_channelNumber][4] == '0'){     //is P pin being used
      if (impedanceCheckValues[_channelNumber][0] < '1') {    //is channel on
        // impedanceCheckValues[i][0] = '1';	//increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
        // channelSettingButtons[i][0].setColorNotPressed(color(25,25,25));
        // writeImpedanceSettings(i);
        initImpWrite(_channelNumber, 'p', '1');
        //initImpWrite
      } else {
        // impedanceCheckValues[i][0] = '0';
        // channelSettingButtons[i][0].setColorNotPressed(color(255));
        // writeImpedanceSettings(i);
        initImpWrite(_channelNumber, 'p', '0');
      }
    }
  }

  // public void mousePressed() {
  //   //if fullChannelController and one of the buttons (other than ON/OFF) is clicked
  //
  //     //if dataSource is coming from OpenBCI, allow user to interact with channel controller
  //   if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
      // if (showFullController) {
      //   for (int i = 0; i < nchan; i++) { //When [i][j] button is clicked
      //     for (int j = 1; j < numSettingsPerChannel; j++) {
      //       if (channelSettingButtons[i][j].isMouseHere()) {
      //         //increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
      //         channelSettingButtons[i][j].wasPressed = true;
      //         channelSettingButtons[i][j].isActive = true;
      //       }
      //     }
      //   }
      // }
  //   }
  //   //on/off button and Imp buttons can always be clicked/released
  //   for (int i = 0; i < nchan; i++) {
  //     if (channelSettingButtons[i][0].isMouseHere()) {
  //       channelSettingButtons[i][0].wasPressed = true;
  //       channelSettingButtons[i][0].isActive = true;
  //     }
  //
  //     //only allow editing of impedance if dataSource == from OpenBCI
  //     if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
  //       if (impedanceCheckButtons[i][0].isMouseHere()) {
  //         impedanceCheckButtons[i][0].wasPressed = true;
  //         impedanceCheckButtons[i][0].isActive = true;
  //       }
  //       if (impedanceCheckButtons[i][1].isMouseHere()) {
  //         impedanceCheckButtons[i][1].wasPressed = true;
  //         impedanceCheckButtons[i][1].isActive = true;
  //       }
  //     }
  //   }
  // }
  //
  // public void mouseReleased() {
  //   //if fullChannelController and one of the buttons (other than ON/OFF) is released
  //   if (showFullController) {
  //     for (int i = 0; i < nchan; i++) { //When [i][j] button is clicked
  //       for (int j = 1; j < numSettingsPerChannel; j++) {
  //         if (channelSettingButtons[i][j].isMouseHere() && channelSettingButtons[i][j].wasPressed == true) {
  //           if (channelSettingValues[i][j] < maxValuesPerSetting[j]) {
  //             channelSettingValues[i][j]++;	//increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
  //           } else {
  //             channelSettingValues[i][j] = '0';
  //           }
  //           // if you're not currently writing a channel and not waiting to rewrite after you've finished mashing the button
  //           if (!openBCI.get_isWritingChannel() && rewriteChannelWhenDoneWriting == false) {
  //             initChannelWrite(i);//write new ADS1299 channel row values to OpenBCI
  //           } else { //else wait until a the current write has finished and then write again ... this is to not overwrite the wrong values while writing a channel
  //             verbosePrint("CONGRATULATIONS, YOU'RE MASHING BUTTONS!");
  //             rewriteChannelWhenDoneWriting = true;
  //             channelToWriteWhenDoneWriting = i;
  //           }
  //         }
  //
  //         // if(!channelSettingButtons[i][j].isMouseHere()){
  //         channelSettingButtons[i][j].isActive = false;
  //         channelSettingButtons[i][j].wasPressed = false;
  //         // }
  //       }
  //     }
  //   }
  //   //ON/OFF button can always be clicked/released
  //   for (int i = 0; i < nchan; i++) {
  //     //was on/off clicked?
  //     if (channelSettingButtons[i][0].isMouseHere() && channelSettingButtons[i][0].wasPressed == true) {
  //       if (channelSettingValues[i][0] < maxValuesPerSetting[0]) {
  //         channelSettingValues[i][0] = '1';	//increment [i][j] channelSettingValue by, until it reaches max values per setting [j],
  //         // channelSettingButtons[i][0].setColorNotPressed(color(25,25,25));
  //         // powerDownChannel(i);
  //         deactivateChannel(i);
  //       } else {
  //         channelSettingValues[i][0] = '0';
  //         // channelSettingButtons[i][0].setColorNotPressed(color(255));
  //         // powerUpChannel(i);
  //         activateChannel(i);
  //       }
  //       // writeChannelSettings(i);//write new ADS1299 channel row values to OpenBCI
  //     }
  //

  //
  //     channelSettingButtons[i][0].isActive = false;
  //     channelSettingButtons[i][0].wasPressed = false;
  //     impedanceCheckButtons[i][0].isActive = false;
  //     impedanceCheckButtons[i][0].wasPressed = false;
  //     impedanceCheckButtons[i][1].isActive = false;
  //     impedanceCheckButtons[i][1].wasPressed = false;
  //   }
  //
  //   update(); //update once to refresh button values
  // }

};
///////////////////////////////////////////////////////////////////////////////
//
// This class configures and manages the connection to the OpenBCI shield for
// the Arduino.  The connection is implemented via a Serial connection.
// The OpenBCI is configured using single letter text commands sent from the
// PC to the Arduino.  The EEG data streams back from the Arduino to the PC
// continuously (once started).  This class defaults to using binary transfer
// for normal operation.
//
// Created: Chip Audette, Oct 2013
// Modified: through April 2014
// Modified again: Conor Russomanno Sept-Oct 2014
// Modified for Daisy (16-chan) OpenBCI V3: Conor Russomanno Nov 2014
// Modified Daisy Behaviors: Chip Audette Dec 2014
//
// Note: this class now expects the data format produced by OpenBCI V3.
//
/////////////////////////////////////////////////////////////////////////////

 //for logging raw bytes to an output file

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

final String command_stop = "s";
// final String command_startText = "x";
final String command_startBinary = "b";
final String command_startBinary_wAux = "n";  // already doing this with 'b' now
final String command_startBinary_4chan = "v";  // not necessary now
final String command_activateFilters = "f";  // swithed from 'F' to 'f'  ... but not necessary because taken out of hardware code
final String command_deactivateFilters = "g";  // not necessary anymore

final String[] command_deactivate_channel = {"1", "2", "3", "4", "5", "6", "7", "8", "q", "w", "e", "r", "t", "y", "u", "i"};
final String[] command_activate_channel = {"!", "@", "#", "$", "%", "^", "&", "*", "Q", "W", "E", "R", "T", "Y", "U", "I"};

int channelDeactivateCounter = 0; //used for re-deactivating channels after switching settings...

boolean threadLock = false;

//these variables are used for "Kill Spikes" ... duplicating the last received data packet if packets were droppeds
boolean werePacketsDropped = false;
int numPacketsDropped = 0;


//everything below is now deprecated...
// final String[] command_activate_leadoffP_channel = {"!", "@", "#", "$", "%", "^", "&", "*"};  //shift + 1-8
// final String[] command_deactivate_leadoffP_channel = {"Q", "W", "E", "R", "T", "Y", "U", "I"};   //letters (plus shift) right below 1-8
// final String[] command_activate_leadoffN_channel = {"A", "S", "D", "F", "G", "H", "J", "K"}; //letters (plus shift) below the letters below 1-8
// final String[] command_deactivate_leadoffN_channel = {"Z", "X", "C", "V", "B", "N", "M", "<"};   //letters (plus shift) below the letters below the letters below 1-8
// final String command_biasAuto = "`";
// final String command_biasFixed = "~";

// ArrayList defaultChannelSettings;

//here is the routine that listens to the serial port.
//if any data is waiting, get it, parse it, and stuff it into our vector of
//pre-allocated dataPacketBuff

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

public void serialEvent(Serial port){
  //check to see which serial port it is
  if (openBCI.isOpenBCISerial(port)) {

    // boolean echoBytes = !openBCI.isStateNormal();
    boolean echoBytes;

    if (openBCI.isStateNormal() != true) {  // || printingRegisters == true){
      echoBytes = true;
    } else {
      echoBytes = false;
    }
    openBCI.read(echoBytes);
    openBCI_byteCount++;
    if (openBCI.get_isNewDataPacketAvailable()) {
      //copy packet into buffer of data packets
      curDataPacketInd = (curDataPacketInd+1) % dataPacketBuff.length; //this is also used to let the rest of the code that it may be time to do something

      openBCI.copyDataPacketTo(dataPacketBuff[curDataPacketInd]);  //resets isNewDataPacketAvailable to false

      // KILL SPIKES!!!
      if(werePacketsDropped){
        for(int i = numPacketsDropped; i > 0; i--){
          int tempDataPacketInd = curDataPacketInd - i; //
          if(tempDataPacketInd >= 0 && tempDataPacketInd < dataPacketBuff.length){
            openBCI.copyDataPacketTo(dataPacketBuff[tempDataPacketInd]);
          } else {
            openBCI.copyDataPacketTo(dataPacketBuff[tempDataPacketInd+255]);
          }
          //put the last stored packet in # of packets dropped after that packet
        }

        //reset werePacketsDropped & numPacketsDropped
        werePacketsDropped = false;
        numPacketsDropped = 0;
      }

      //If networking enabled --> send data every sample if 8 channels or every other sample if 16 channels
      if (networkType !=0) {
        if (nchan==8) {
          sendRawData_dataPacket(dataPacketBuff[curDataPacketInd], openBCI.get_scale_fac_uVolts_per_count(), openBCI.get_scale_fac_accel_G_per_count());
        } else if ((nchan==16) && ((dataPacketBuff[curDataPacketInd].sampleIndex %2)!=1)) {
          sendRawData_dataPacket(dataPacketBuff[curDataPacketInd], openBCI.get_scale_fac_uVolts_per_count(), openBCI.get_scale_fac_accel_G_per_count());
        }
      }
      switch (outputDataSource) {
      case OUTPUT_SOURCE_ODF:
        fileoutput_odf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd], openBCI.get_scale_fac_uVolts_per_count(), openBCI.get_scale_fac_accel_G_per_count());
        break;
      case OUTPUT_SOURCE_BDF:
        curBDFDataPacketInd = curDataPacketInd;
        thread("writeRawData_dataPacket_bdf");
        // fileoutput_bdf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd]);
        break;
      case OUTPUT_SOURCE_NONE:
      default:
        // Do nothing...
        break;
      }

      newPacketCounter++;
    }
  } else {

    //Used for serial communications, primarily everything in no_start_connection
    if (no_start_connection) {


      if (board_message == null || dollaBillz>2) {
        board_message = new StringBuilder();
        dollaBillz = 0;
      }

      inByte = PApplet.parseByte(port.read());
      if (PApplet.parseChar(inByte) == 'S' || PApplet.parseChar(inByte) == 'F') isOpenBCI = true;

      // print(char(inByte));
      if (inByte != -1) {
        if (isGettingPoll) {
          if (inByte != '$') {
            if (!spaceFound) board_message.append(PApplet.parseChar(inByte));
            else hexToInt = Integer.parseInt(String.format("%02X", inByte), 16);

            if (PApplet.parseChar(inByte) == ' ') spaceFound = true;
          } else dollaBillz++;
        } else {
          if (inByte != '$') board_message.append(PApplet.parseChar(inByte));
          else dollaBillz++;
        }
      }
    } else {
      //println("Recieved serial data not from OpenBCI"); //this is a bit of a lie
      inByte = PApplet.parseByte(port.read());
      if (isOpenBCI) {

        if (board_message == null || dollaBillz >2) {
          board_message = new StringBuilder();
          dollaBillz=0;
        }
        if(inByte != '$'){
          board_message.append(PApplet.parseChar(inByte));
        } else { dollaBillz++; }
      } else if(PApplet.parseChar(inByte) == 'S' || PApplet.parseChar(inByte) == 'F'){
        isOpenBCI = true;
        if(board_message == null){
          board_message = new StringBuilder();
          board_message.append(PApplet.parseChar(inByte));
        }
      }
    }
  }
}

public void writeRawData_dataPacket_bdf() {
  fileoutput_bdf.writeRawData_dataPacket(dataPacketBuff[curBDFDataPacketInd]);
}

public void startRunning() {
  verbosePrint("startRunning...");
  output("Data stream started.");
  if (eegDataSource == DATASOURCE_GANGLION) {
    if (ganglion != null) {
      ganglion.startDataTransfer();
    }
  } else {
    if (openBCI != null) {
      openBCI.startDataTransfer();
    }
  }

  isRunning = true;
}

public void stopRunning() {
  // openBCI.changeState(0); //make sure it's no longer interpretting as binary
  verbosePrint(appName + ": stopRunning: stop running...");
  output("Data stream stopped.");
  if (eegDataSource == DATASOURCE_GANGLION) {
    if (ganglion != null) {
      ganglion.stopDataTransfer();
    }
  } else {
    if (openBCI != null) {
      openBCI.stopDataTransfer();
    }
  }

  timeSinceStopRunning = millis(); //used as a timer to prevent misc. bytes from flooding serial...
  isRunning = false;
  // openBCI.changeState(0); //make sure it's no longer interpretting as binary
  // systemMode = 0;
  // closeLogFile();
}

//execute this function whenver the stop button is pressed
public void stopButtonWasPressed() {
  //toggle the data transfer state of the ADS1299...stop it or start it...
  if (isRunning) {
    verbosePrint(appName + ": stopButton was pressed...stopping data transfer...");
    wm.setUpdating(false);
    stopRunning();
    topNav.stopButton.setString(topNav.stopButton_pressToStart_txt);
    topNav.stopButton.setColorNotPressed(color(184, 220, 105));
    if (eegDataSource == DATASOURCE_GANGLION && ganglion.isCheckingImpedance()) {
      ganglion.impedanceStop();
      w_ganglionImpedance.startStopCheck.but_txt = "Start Impedance Check";
    }
  } else { //not running
    verbosePrint(appName + ": startButton was pressed...starting data transfer...");
    wm.setUpdating(true);
    startRunning();
    topNav.stopButton.setString(topNav.stopButton_pressToStop_txt);
    topNav.stopButton.setColorNotPressed(color(224, 56, 45));
    nextPlayback_millis = millis();  //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
    if (eegDataSource == DATASOURCE_GANGLION && ganglion.isCheckingImpedance()) {
      ganglion.impedanceStop();
      w_ganglionImpedance.startStopCheck.but_txt = "Start Impedance Check";
    }
  }
}

public void printRegisters() {
  openBCI.printRegisters();
}

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------

class OpenBCI_ADS1299 {

  //final static int DATAMODE_TXT = 0;
  final static int DATAMODE_BIN = 2;
  final static int DATAMODE_BIN_WAUX = 1;  //switched to this value so that receiving Accel data is now the default
  //final static int DATAMODE_BIN_4CHAN = 4;

  final static int STATE_NOCOM = 0;
  final static int STATE_COMINIT = 1;
  final static int STATE_SYNCWITHHARDWARE = 2;
  final static int STATE_NORMAL = 3;
  final static int STATE_STOPPED = 4;
  final static int COM_INIT_MSEC = 3000; //you may need to vary this for your computer or your Arduino

  //int[] measured_packet_length = {0,0,0,0,0};
  //int measured_packet_length_ind = 0;
  //int known_packet_length_bytes = 0;

  final static byte BYTE_START = (byte)0xA0;
  final static byte BYTE_END = (byte)0xC0;

  //here is the serial port for this OpenBCI board
  private Serial serial_openBCI = null;
  private boolean portIsOpen = false;

  int prefered_datamode = DATAMODE_BIN_WAUX;

  private int state = STATE_NOCOM;
  int dataMode = -1;
  int prevState_millis = 0;

  private int nEEGValuesPerPacket = 8; //defined by the data format sent by openBCI boards
  //int nAuxValuesPerPacket = 3; //defined by the data format sent by openBCI boards
  private DataPacket_ADS1299 rawReceivedDataPacket;
  private DataPacket_ADS1299 missedDataPacket;
  private DataPacket_ADS1299 dataPacket;
  public int [] validAuxValues = {0, 0, 0};
  public boolean[] freshAuxValuesAvailable = {false, false, false};
  public boolean freshAuxValues = false;
  //DataPacket_ADS1299 prevDataPacket;

  private int nAuxValues;
  private boolean isNewDataPacketAvailable = false;
  private OutputStream output; //for debugging  WEA 2014-01-26
  private int prevSampleIndex = 0;
  private int serialErrorCounter = 0;

  private final float fs_Hz = 250.0f;  //sample rate used by OpenBCI board...set by its Arduino code
  private final float ADS1299_Vref = 4.5f;  //reference voltage for ADC in ADS1299.  set by its hardware
  private float ADS1299_gain = 24.0f;  //assumed gain setting for ADS1299.  set by its Arduino code
  private float openBCI_series_resistor_ohms = 2200; // Ohms. There is a series resistor on the 32 bit board.
  private float scale_fac_uVolts_per_count = ADS1299_Vref / ((float)(pow(2, 23)-1)) / ADS1299_gain  * 1000000.f; //ADS1299 datasheet Table 7, confirmed through experiment
  //float LIS3DH_full_scale_G = 4;  // +/- 4G, assumed full scale setting for the accelerometer
  private final float scale_fac_accel_G_per_count = 0.002f / ((float)pow(2, 4));  //assume set to +/4G, so 2 mG per digit (datasheet). Account for 4 bits unused
  //final float scale_fac_accel_G_per_count = 1.0;
  private final float leadOffDrive_amps = 6.0e-9f;  //6 nA, set by its Arduino code
  private final String failureMessage = "Failure: Communications timeout - Device failed to poll Host";

  boolean isBiasAuto = true; //not being used?

  //data related to Conor's setup for V3 boards
  final char[] EOT = {'$', '$', '$'};
  char[] prev3chars = {'#', '#', '#'};
  private String potentialFailureMessage = "";
  private String defaultChannelSettings = "";
  private String daisyOrNot = "";
  private int hardwareSyncStep = 0; //start this at 0...
  private boolean readyToSend = false; //system waits for $$$ after requesting information from OpenBCI board
  private long timeOfLastCommand = 0; //used when sync'ing to hardware

  //some get methods
  public float get_fs_Hz() {
    return fs_Hz;
  }
  public float get_Vref() {
    return ADS1299_Vref;
  }
  public void set_ADS1299_gain(float _gain) {
    ADS1299_gain = _gain;
    scale_fac_uVolts_per_count = ADS1299_Vref / ((float)(pow(2, 23)-1)) / ADS1299_gain  * 1000000.0f; //ADS1299 datasheet Table 7, confirmed through experiment
  }
  public float get_ADS1299_gain() {
    return ADS1299_gain;
  }
  public float get_series_resistor() {
    return openBCI_series_resistor_ohms;
  }
  public float get_scale_fac_uVolts_per_count() {
    return scale_fac_uVolts_per_count;
  }
  public float get_scale_fac_accel_G_per_count() {
    return scale_fac_accel_G_per_count;
  }
  public float get_leadOffDrive_amps() {
    return leadOffDrive_amps;
  }
  public String get_defaultChannelSettings() {
    return defaultChannelSettings;
  }
  public int get_state() {
    return state;
  };
  public boolean get_isNewDataPacketAvailable() {
    return isNewDataPacketAvailable;
  }

  //constructors
  OpenBCI_ADS1299() {
  };  //only use this if you simply want access to some of the constants
  OpenBCI_ADS1299(PApplet applet, String comPort, int baud, int nEEGValuesPerOpenBCI, boolean useAux, int nAuxValuesPerPacket) {
    nAuxValues=nAuxValuesPerPacket;

    //choose data mode
    println("OpenBCI_ADS1299: prefered_datamode = " + prefered_datamode + ", nValuesPerPacket = " + nEEGValuesPerPacket);
    if (prefered_datamode == DATAMODE_BIN_WAUX) {
      if (!useAux) {
        //must be requesting the aux data, so change the referred data mode
        prefered_datamode = DATAMODE_BIN;
        nAuxValues = 0;
        //println("OpenBCI_ADS1299: nAuxValuesPerPacket = " + nAuxValuesPerPacket + " so setting prefered_datamode to " + prefered_datamode);
      }
    }

    println("OpenBCI_ADS1299: a");

    dataMode = prefered_datamode;

    println("nEEGValuesPerPacket = " + nEEGValuesPerPacket);
    println("nEEGValuesPerOpenBCI = " + nEEGValuesPerOpenBCI);

    //allocate space for data packet
    rawReceivedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    missedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    dataPacket = new DataPacket_ADS1299(nEEGValuesPerOpenBCI, nAuxValuesPerPacket);            //this could be 8 or 16 channels
    //prevDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket,nAuxValuesPerPacket);
    //set all values to 0 so not null

    println("(2) nEEGValuesPerPacket = " + nEEGValuesPerPacket);
    println("(2) nEEGValuesPerOpenBCI = " + nEEGValuesPerOpenBCI);
    println("missedDataPacket.values.length = " + missedDataPacket.values.length);

    for (int i = 0; i < nEEGValuesPerPacket; i++) {
      rawReceivedDataPacket.values[i] = 0;
      //prevDataPacket.values[i] = 0;
    }

    // %%%%% HAD TO KILL THIS ... not sure why nEEGValuesPerOpenBCI would ever loop to 16... this may be an incongruity due to the way we kludge the 16chan data in 2 packets...
    // for (int i=0; i < nEEGValuesPerOpenBCI; i++) {
    //   println("i = " + i);
    //   dataPacket.values[i] = 0;
    //   missedDataPacket.values[i] = 0;
    // }

    for (int i=0; i < nEEGValuesPerPacket; i++) {
      // println("i = " + i);
      dataPacket.values[i] = 0;
      missedDataPacket.values[i] = 0;
    }
    for (int i = 0; i < nAuxValuesPerPacket; i++) {
      rawReceivedDataPacket.auxValues[i] = 0;
      dataPacket.auxValues[i] = 0;
      missedDataPacket.auxValues[i] = 0;
      //prevDataPacket.auxValues[i] = 0;
    }

    println("OpenBCI_ADS1299: b");

    //prepare the serial port  ... close if open
    //println("OpenBCI_ADS1299: port is open? ... " + portIsOpen);
    //if(portIsOpen == true) {
    if (isSerialPortOpen()) {
      closeSerialPort();
    }

    println("OpenBCI_ADS1299: i = " + millis());
    openSerialPort(applet, comPort, baud);
    println("OpenBCI_ADS1299: j = " + millis());

    //open file for raw bytes
    //output = createOutput("rawByteDumpFromProcessing.bin");  //for debugging  WEA 2014-01-26
  }

  // //manage the serial port
  private int openSerialPort(PApplet applet, String comPort, int baud) {

    output("Attempting to open Serial/COM port: " + openBCI_portName);
    try {
      println("OpenBCI_ADS1299: openSerialPort: attempting to open serial port: " + openBCI_portName);
      serial_openBCI = new Serial(applet, comPort, baud); //open the com port
      serial_openBCI.clear(); // clear anything in the com port's buffer
      portIsOpen = true;
      println("OpenBCI_ADS1299: openSerialPort: port is open (t)? ... " + portIsOpen);
      changeState(STATE_COMINIT);
      return 0;
    }
    catch (RuntimeException e) {
      if (e.getMessage().contains("<init>")) {
        serial_openBCI = null;
        System.out.println("OpenBCI_ADS1299: openSerialPort: port in use, trying again later...");
        portIsOpen = false;
      } else {
        println("RunttimeException: " + e);
        output("Error connecting to selected Serial/COM port. Make sure your board is powered up and your dongle is plugged in.");
        abandonInit = true; //global variable in DAC_GUI.pde
      }
      return 0;
    }
  }

  public int changeState(int newState) {
    state = newState;
    prevState_millis = millis();
    return 0;
  }

  public int finalizeCOMINIT() {
    // //wait specified time for COM/serial port to initialize
    // if (state == STATE_COMINIT) {
    //   // println("OpenBCI_ADS1299: finalizeCOMINIT: Initializing Serial: millis() = " + millis());
    //   if ((millis() - prevState_millis) > COM_INIT_MSEC) {
    //     //serial_openBCI.write(command_activates + "\n"); println("Processing: OpenBCI_ADS1299: activating filters");
    //     println("OpenBCI_ADS1299: finalizeCOMINIT: State = NORMAL");
    changeState(STATE_NORMAL);
    //     // startRunning();
    //   }
    // }
    return 0;
  }

  public int closeSDandSerialPort() {
    int returnVal=0;

    closeSDFile();

    readyToSend = false;
    returnVal = closeSerialPort();
    prevState_millis = 0;  //reset OpenBCI_ADS1299 state clock to use as a conditional for timing at the beginnign of systemUpdate()
    hardwareSyncStep = 0; //reset Hardware Sync step to be ready to go again...

    return returnVal;
  }

  public int closeSDFile() {
    println("Closing any open SD file. Writing 'j' to OpenBCI.");
    if (isSerialPortOpen()) serial_openBCI.write("j"); // tell the SD file to close if one is open...
    delay(100); //make sure 'j' gets sent to the board
    return 0;
  }

  public int closeSerialPort() {
    // if (serial_openBCI != null) {
    portIsOpen = false;
    if (serial_openBCI != null) {
      serial_openBCI.stop();
    }
    serial_openBCI = null;
    state = STATE_NOCOM;
    println("OpenBCI_ADS1299: closeSerialPort: closed");
    return 0;
  }

  public void syncWithHardware(int sdSetting) {
    switch (hardwareSyncStep) {
      // case 1:
      //   println(appName + ": syncWithHardware: [0] Sending 'v' to OpenBCI to reset hardware in case of 32bit board...");
      //   serial_openBCI.write('v');
      //   readyToSend = false; //wait for $$$ to iterate... applies to commands expecting a response
    case 1: //send # of channels (8 or 16) ... (regular or daisy setup)
      println("OpenBCI_ADS1299: syncWithHardware: [1] Sending channel count (" + nchan + ") to OpenBCI...");
      if (nchan == 8) {
        serial_openBCI.write('c');
      }
      if (nchan == 16) {
        serial_openBCI.write('C');
        readyToSend = false;
      }
      break;
    case 2: //reset hardware to default registers
      println("OpenBCI_ADS1299: syncWithHardware: [2] Reseting OpenBCI registers to default... writing \'d\'...");
      serial_openBCI.write("d");
      break;
    case 3: //ask for series of channel setting ASCII values to sync with channel setting interface in GUI
      println("OpenBCI_ADS1299: syncWithHardware: [3] Retrieving OpenBCI's channel settings to sync with GUI... writing \'D\'... waiting for $$$...");
      readyToSend = false; //wait for $$$ to iterate... applies to commands expecting a response
      serial_openBCI.write("D");
      break;
    case 4: //check existing registers
      println("OpenBCI_ADS1299: syncWithHardware: [4] Retrieving OpenBCI's full register map for verification... writing \'?\'... waiting for $$$...");
      readyToSend = false; //wait for $$$ to iterate... applies to commands expecting a response
      serial_openBCI.write("?");
      break;
    case 5:
      // serial_openBCI.write("j"); // send OpenBCI's 'j' commaned to make sure any already open SD file is closed before opening another one...
      switch (sdSetting) {
      case 0: //"Do not write to SD"
        //do nothing
        break;
      case 1: //"5 min max"
        serial_openBCI.write("A");
        break;
      case 2: //"5 min max"
        serial_openBCI.write("S");
        break;
      case 3: //"5 min max"
        serial_openBCI.write("F");
        break;
      case 4: //"5 min max"
        serial_openBCI.write("G");
        break;
      case 5: //"5 min max"
        serial_openBCI.write("H");
        break;
      case 6: //"5 min max"
        serial_openBCI.write("J");
        break;
      case 7: //"5 min max"
        serial_openBCI.write("K");
        break;
      case 8: //"5 min max"
        serial_openBCI.write("L");
        break;
      }
      println("OpenBCI_ADS1299: syncWithHardware: [5] Writing selected SD setting (" + sdSettingString + ") to OpenBCI...");
      if (sdSetting != 0) {
        readyToSend = false; //wait for $$$ to iterate... applies to commands expecting a response
      }
      //final hacky way of abandoning initiation if someone selected daisy but doesn't have one connected.
      if(abandonInit){
        haltSystem();
        output("No daisy board present. Make sure you selected the correct number of channels.");
        controlPanel.open();
        abandonInit = false;
      }
      break;
    case 6:
      output("OpenBCI_ADS1299: syncWithHardware: The GUI is done intializing. Click outside of the control panel to interact with the GUI.");
      changeState(STATE_STOPPED);
      systemMode = 10;
      controlPanel.close();
      topNav.controlPanelCollapser.setIsActive(false);
      //renitialize GUI if nchan has been updated... needs to be built
      break;
    }
  }

  public void updateSyncState(int sdSetting) {
    //has it been 3000 milliseconds since we initiated the serial port? We want to make sure we wait for the OpenBCI board to finish its setup()
    // println("0");

    if ( (millis() - prevState_millis > COM_INIT_MSEC) && (prevState_millis != 0) && (state == openBCI.STATE_COMINIT) ) {
      state = STATE_SYNCWITHHARDWARE;
      timeOfLastCommand = millis();
      serial_openBCI.clear();
      potentialFailureMessage = "";
      defaultChannelSettings = ""; //clear channel setting string to be reset upon a new Init System
      daisyOrNot = ""; //clear daisyOrNot string to be reset upon a new Init System
      println("OpenBCI_ADS1299: systemUpdate: [0] Sending 'v' to OpenBCI to reset hardware in case of 32bit board...");
      serial_openBCI.write('v');
    }

    //if we are in SYNC WITH HARDWARE state ... trigger a command
    if ( (state == STATE_SYNCWITHHARDWARE) && (currentlySyncing == false) ) {
      if (millis() - timeOfLastCommand > 200 && readyToSend == true) {
        timeOfLastCommand = millis();
        hardwareSyncStep++;
        syncWithHardware(sdSetting);
      }
    }
  }

  public void sendChar(char val) {
    if (isSerialPortOpen()) {
      serial_openBCI.write(key);//send the value as ascii (with a newline character?)
    }
  }

  public void startDataTransfer() {
    if (isSerialPortOpen()) {
      serial_openBCI.clear(); // clear anything in the com port's buffer
      // stopDataTransfer();
      changeState(STATE_NORMAL);  // make sure it's now interpretting as binary
      println("OpenBCI_ADS1299: startDataTransfer(): writing \'" + command_startBinary + "\' to the serial port...");
      serial_openBCI.write(command_startBinary);
    }
  }

  public void stopDataTransfer() {
    if (isSerialPortOpen()) {
      serial_openBCI.clear(); // clear anything in the com port's buffer
      openBCI.changeState(STATE_STOPPED);  // make sure it's now interpretting as binary
      println("OpenBCI_ADS1299: startDataTransfer(): writing \'" + command_stop + "\' to the serial port...");
      serial_openBCI.write(command_stop);// + "\n");
    }
  }

  public boolean isSerialPortOpen() {
    if (portIsOpen & (serial_openBCI != null)) {
      return true;
    } else {
      return false;
    }
  }
  public boolean isOpenBCISerial(Serial port) {
    if (serial_openBCI == port) {
      return true;
    } else {
      return false;
    }
  }

  public void printRegisters() {
    if (isSerialPortOpen()) {
      println("OpenBCI_ADS1299: printRegisters(): Writing ? to OpenBCI...");
      openBCI.serial_openBCI.write('?');
    }
  }

  //read from the serial port
  public int read() {
    return read(false);
  }
  public int read(boolean echoChar) {
    //println("OpenBCI_ADS1299: read(): State: " + state);
    //get the byte
    byte inByte;
    if (isSerialPortOpen()) {
      inByte = PApplet.parseByte(serial_openBCI.read());
    } else {
      println("Serial port not open aborting.");
      return 0;
    }


    //write the most recent char to the console
    // If the GUI is in streaming mode then echoChar will be false
    if (echoChar) {  //if not in interpret binary (NORMAL) mode
      // print("hardwareSyncStep: "); println(hardwareSyncStep);
      // print(".");
      char inASCII = PApplet.parseChar(inByte);
      if (isRunning == false && (millis() - timeSinceStopRunning) > 500) {
        print(PApplet.parseChar(inByte));
      }

      //keep track of previous three chars coming from OpenBCI
      prev3chars[0] = prev3chars[1];
      prev3chars[1] = prev3chars[2];
      prev3chars[2] = inASCII;

      if (hardwareSyncStep == 0 && inASCII != '$') {
        potentialFailureMessage+=inASCII;
      }

      if (hardwareSyncStep == 1 && inASCII != '$') {
        daisyOrNot+=inASCII;
        //if hardware returns 8 because daisy is not attached, switch the GUI mode back to 8 channels
        // if(nchan == 16 && char(daisyOrNot.substring(daisyOrNot.length() - 1)) == '8'){
        if (nchan == 16 && daisyOrNot.charAt(daisyOrNot.length() - 1) == '8') {
          // verbosePrint(" received from OpenBCI... Switching to nchan = 8 bc daisy is not present...");
          verbosePrint(" received from OpenBCI... Abandoning hardware initiation.");
          abandonInit = true;
          // haltSystem();

          // updateToNChan(8);
          //
          // //initialize the FFT objects
          // for (int Ichan=0; Ichan < nchan; Ichan++) {
          //   verbosePrint("Init FFT Buff \u2013 "+Ichan);
          //   fftBuff[Ichan] = new FFT(Nfft, get_fs_Hz_safe());
          // }  //make the FFT objects
          //
          // initializeFFTObjects(fftBuff, dataBuffY_uV, Nfft, get_fs_Hz_safe());
          // setupWidgetManager();
        }
      }

      if (hardwareSyncStep == 3 && inASCII != '$') { //if we're retrieving channel settings from OpenBCI
        defaultChannelSettings+=inASCII;
      }

      //if the last three chars are $$$, it means we are moving on to the next stage of initialization
      if (prev3chars[0] == EOT[0] && prev3chars[1] == EOT[1] && prev3chars[2] == EOT[2]) {
        // verbosePrint(" > EOT detected...");
        // Added for V2 system down rejection line
        if (hardwareSyncStep == 0) {
          // Failure: Communications timeout - Device failed to poll Host$$$
          if (potentialFailureMessage.equals(failureMessage)) {
            closeLogFile();
            return 0;
          }
        }
        // hardwareSyncStep++;
        prev3chars[2] = '#';
        if (hardwareSyncStep == 3) {
          println("OpenBCI_ADS1299: read(): x");
          println(defaultChannelSettings);
          println("OpenBCI_ADS1299: read(): y");
          // gui.cc.loadDefaultChannelSettings();
          w_timeSeries.hsc.loadDefaultChannelSettings();
          println("OpenBCI_ADS1299: read(): z");
        }
        readyToSend = true;
        // println(hardwareSyncStep);
        // syncWithHardware(); //haha, I'm getting very verbose with my naming... it's late...
      }
    }

    //write raw unprocessed bytes to a binary data dump file
    if (output != null) {
      try {
        output.write(inByte);   //for debugging  WEA 2014-01-26
      }
      catch (IOException e) {
        System.err.println("OpenBCI_ADS1299: read(): Caught IOException: " + e.getMessage());
        //do nothing
      }
    }

    interpretBinaryStream(inByte);  //new 2014-02-02 WEA
    return PApplet.parseInt(inByte);
  }

  /* **** Borrowed from Chris Viegl from his OpenBCI parser for BrainBay
   Modified by Joel Murphy and Conor Russomanno to read OpenBCI data
   Packet Parser for OpenBCI (1-N channel binary format):
   3-byte data values are stored in 'little endian' formant in AVRs
   so this protocol parser expects the lower bytes first.
   Start Indicator: 0xA0
   EXPECTING STANDARD PACKET LENGTH DON'T NEED: Packet_length  : 1 byte  (length = 4 bytes framenumber + 4 bytes per active channel + (optional) 4 bytes for 1 Aux value)
   Framenumber     : 1 byte (Sequential counter of packets)
   Channel 1 data  : 3 bytes
   ...
   Channel 8 data  : 3 bytes
   Aux Values      : UP TO 6 bytes
   End Indcator    : 0xC0
   TOTAL OF 33 bytes ALL DAY
   ********************************************************************* */
  private int nDataValuesInPacket = 0;
  private int localByteCounter=0;
  private int localChannelCounter=0;
  private int PACKET_readstate = 0;
  // byte[] localByteBuffer = {0,0,0,0};
  private byte[] localAdsByteBuffer = {0, 0, 0};
  private byte[] localAccelByteBuffer = {0, 0};

  public void interpretBinaryStream(byte actbyte) {
    boolean flag_copyRawDataToFullData = false;

    //println("OpenBCI_ADS1299: interpretBinaryStream: PACKET_readstate " + PACKET_readstate);
    switch (PACKET_readstate) {
    case 0:
      //look for header byte
      if (actbyte == PApplet.parseByte(0xA0)) {          // look for start indicator
        // println("OpenBCI_ADS1299: interpretBinaryStream: found 0xA0");
        PACKET_readstate++;
      }
      break;
    case 1:
      //check the packet counter
      // println("case 1");
      byte inByte = actbyte;
      rawReceivedDataPacket.sampleIndex = PApplet.parseInt(inByte); //changed by JAM
      if ((rawReceivedDataPacket.sampleIndex-prevSampleIndex) != 1) {
        if (rawReceivedDataPacket.sampleIndex != 0) {  // if we rolled over, don't count as error
          serialErrorCounter++;
          werePacketsDropped = true; //set this true to activate packet duplication in serialEvent

          if(rawReceivedDataPacket.sampleIndex < prevSampleIndex){   //handle the situation in which the index jumps from 250s past 255, and back to 0
            numPacketsDropped = (rawReceivedDataPacket.sampleIndex+255) - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
          } else {
            numPacketsDropped = rawReceivedDataPacket.sampleIndex - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
          }

          println("OpenBCI_ADS1299: apparent sampleIndex jump from Serial data: " + prevSampleIndex + " to  " + rawReceivedDataPacket.sampleIndex + ".  Keeping packet. (" + serialErrorCounter + ")");
          if (outputDataSource == OUTPUT_SOURCE_BDF) {
            int fakePacketsToWrite = (rawReceivedDataPacket.sampleIndex - prevSampleIndex) - 1;
            for (int i = 0; i < fakePacketsToWrite; i++) {
              fileoutput_bdf.writeRawData_dataPacket(missedDataPacket);
            }
            println("OpenBCI_ADS1299: because BDF, wrote " + fakePacketsToWrite + " empty data packet(s)");
          }
        }
      }
      prevSampleIndex = rawReceivedDataPacket.sampleIndex;
      localByteCounter=0;//prepare for next usage of localByteCounter
      localChannelCounter=0; //prepare for next usage of localChannelCounter
      PACKET_readstate++;
      break;
    case 2:
      // get ADS channel values
      // println("case 2");
      localAdsByteBuffer[localByteCounter] = actbyte;
      localByteCounter++;
      if (localByteCounter==3) {
        rawReceivedDataPacket.values[localChannelCounter] = interpret24bitAsInt32(localAdsByteBuffer);
        arrayCopy(localAdsByteBuffer, rawReceivedDataPacket.rawValues[localChannelCounter]);
        localChannelCounter++;
        if (localChannelCounter==8) { //nDataValuesInPacket) {
          // all ADS channels arrived !
          // println("OpenBCI_ADS1299: interpretBinaryStream: localChannelCounter = " + localChannelCounter);
          PACKET_readstate++;
          if (prefered_datamode != DATAMODE_BIN_WAUX) PACKET_readstate++;  //if not using AUX, skip over the next readstate
          localByteCounter = 0;
          localChannelCounter = 0;
          //isNewDataPacketAvailable = true;  //tell the rest of the code that the data packet is complete
        } else {
          //prepare for next data channel
          localByteCounter=0; //prepare for next usage of localByteCounter
        }
      }
      break;
    case 3:
      // get LIS3DH channel values 2 bytes times 3 axes
      // println("case 3");
      localAccelByteBuffer[localByteCounter] = actbyte;
      localByteCounter++;
      if (localByteCounter==2) {
        rawReceivedDataPacket.auxValues[localChannelCounter]  = interpret16bitAsInt32(localAccelByteBuffer);
        arrayCopy(localAccelByteBuffer, rawReceivedDataPacket.rawAuxValues[localChannelCounter]);
        if (rawReceivedDataPacket.auxValues[localChannelCounter] != 0) {
          validAuxValues[localChannelCounter] = rawReceivedDataPacket.auxValues[localChannelCounter];
          freshAuxValuesAvailable[localChannelCounter] = true;
          freshAuxValues = true;
        } else freshAuxValues = false;
        localChannelCounter++;
        if (localChannelCounter==nAuxValues) { //number of accelerometer axis) {
          // all Accelerometer channels arrived !
          // println("OpenBCI_ADS1299: interpretBinaryStream: Accel Data: " + rawReceivedDataPacket.auxValues[0] + ", " + rawReceivedDataPacket.auxValues[1] + ", " + rawReceivedDataPacket.auxValues[2]);
          PACKET_readstate++;
          localByteCounter = 0;
          //isNewDataPacketAvailable = true;  //tell the rest of the code that the data packet is complete
        } else {
          //prepare for next data channel
          localByteCounter=0; //prepare for next usage of localByteCounter
        }
      }
      break;
    case 4:
      //look for end byte
      // println("case 4");
      if (actbyte == PApplet.parseByte(0xC0) || actbyte == PApplet.parseByte(0xC1)) {    // if correct end delimiter found:
        // println("... 0xCx found");
        // println("OpenBCI_ADS1299: interpretBinaryStream: found end byte. Setting isNewDataPacketAvailable to TRUE");
        isNewDataPacketAvailable = true; //original place for this.  but why not put it in the previous case block
        flag_copyRawDataToFullData = true;  //time to copy the raw data packet into the full data packet (mainly relevant for 16-chan OpenBCI)
      } else {
        serialErrorCounter++;
        println("OpenBCI_ADS1299: interpretBinaryStream: Actbyte = " + actbyte);
        println("OpenBCI_ADS1299: interpretBinaryStream: expecteding end-of-packet byte is missing.  Discarding packet. (" + serialErrorCounter + ")");
      }
      PACKET_readstate=0;  // either way, look for next packet
      break;
    default:
      println("OpenBCI_ADS1299: interpretBinaryStream: Unknown byte: " + actbyte + " .  Continuing...");
      PACKET_readstate=0;  // look for next packet
    }

    if (flag_copyRawDataToFullData) {
      copyRawDataToFullData();
    }
  } // end of interpretBinaryStream


  //activate or deactivate an EEG channel...channel counting is zero through nchan-1
  public void changeChannelState(int Ichan, boolean activate) {
    if (isSerialPortOpen()) {
      // if ((Ichan >= 0) && (Ichan < command_activate_channel.length)) {
      if ((Ichan >= 0)) {
        if (activate) {
          // serial_openBCI.write(command_activate_channel[Ichan]);
          // gui.cc.powerUpChannel(Ichan);
          w_timeSeries.hsc.powerUpChannel(Ichan);
        } else {
          // serial_openBCI.write(command_deactivate_channel[Ichan]);
          // gui.cc.powerDownChannel(Ichan);
          w_timeSeries.hsc.powerDownChannel(Ichan);
        }
      }
    }
  }

  //deactivate an EEG channel...channel counting is zero through nchan-1
  public void deactivateChannel(int Ichan) {
    if (isSerialPortOpen()) {
      if ((Ichan >= 0) && (Ichan < command_deactivate_channel.length)) {
        serial_openBCI.write(command_deactivate_channel[Ichan]);
      }
    }
  }

  //activate an EEG channel...channel counting is zero through nchan-1
  public void activateChannel(int Ichan) {
    if (isSerialPortOpen()) {
      if ((Ichan >= 0) && (Ichan < command_activate_channel.length)) {
        serial_openBCI.write(command_activate_channel[Ichan]);
      }
    }
  }

  //return the state
  public boolean isStateNormal() {
    if (state == STATE_NORMAL) {
      return true;
    } else {
      return false;
    }
  }

  private int interpret24bitAsInt32(byte[] byteArray) {
    //little endian
    int newInt = (
      ((0xFF & byteArray[0]) << 16) |
      ((0xFF & byteArray[1]) << 8) |
      (0xFF & byteArray[2])
      );
    if ((newInt & 0x00800000) > 0) {
      newInt |= 0xFF000000;
    } else {
      newInt &= 0x00FFFFFF;
    }
    return newInt;
  }

  private int interpret16bitAsInt32(byte[] byteArray) {
    int newInt = (
      ((0xFF & byteArray[0]) << 8) |
      (0xFF & byteArray[1])
      );
    if ((newInt & 0x00008000) > 0) {
      newInt |= 0xFFFF0000;
    } else {
      newInt &= 0x0000FFFF;
    }
    return newInt;
  }


  private int copyRawDataToFullData() {
    //Prior to the 16-chan OpenBCI, we did NOT have rawReceivedDataPacket along with dataPacket...we just had dataPacket.
    //With the 16-chan OpenBCI, where the first 8 channels are sent and then the second 8 channels are sent, we introduced
    //this extra structure so that we could alternate between them.
    //
    //This function here decides how to join the latest data (rawReceivedDataPacket) into the full dataPacket

    if (dataPacket.values.length < 2*rawReceivedDataPacket.values.length) {
      //this is an 8 channel board, so simply copy the data
      return rawReceivedDataPacket.copyTo(dataPacket);
    } else {
      //this is 16-channels, so copy the raw data into the correct channels of the new data
      int offsetInd_values = 0;  //this is correct assuming we just recevied a  "board" packet (ie, channels 1-8)
      int offsetInd_aux = 0;     //this is correct assuming we just recevied a  "board" packet (ie, channels 1-8)
      if (rawReceivedDataPacket.sampleIndex % 2 == 0) { // even data packets are from the daisy board
        offsetInd_values = rawReceivedDataPacket.values.length;  //start copying to the 8th slot
        //offsetInd_aux = rawReceivedDataPacket.auxValues.length;  //start copying to the 3rd slot
        offsetInd_aux = 0;
      }
      return rawReceivedDataPacket.copyTo(dataPacket, offsetInd_values, offsetInd_aux);
    }
  }

  public int copyDataPacketTo(DataPacket_ADS1299 target) {
    isNewDataPacketAvailable = false;
    return dataPacket.copyTo(target);
  }


  private long timeOfLastChannelWrite = 0;
  private int channelWriteCounter = 0;
  private boolean isWritingChannel = false;
  public boolean get_isWritingChannel() {
    return isWritingChannel;
  }
  public void configureAllChannelsToDefault() {
    serial_openBCI.write('d');
  };
  public void initChannelWrite(int _numChannel) {  //numChannel counts from zero
    timeOfLastChannelWrite = millis();
    isWritingChannel = true;
  }

  // FULL DISCLAIMER: this method is messy....... very messy... we had to brute force a firmware miscue
  public void writeChannelSettings(int _numChannel, char[][] channelSettingValues) {   //numChannel counts from zero
    if (millis() - timeOfLastChannelWrite >= 50) { //wait 50 milliseconds before sending next character
      verbosePrint("---");
      switch (channelWriteCounter) {
      case 0: //start sequence by send 'x'
        verbosePrint("x" + " :: " + millis());
        serial_openBCI.write('x');
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 1: //send channel number
        verbosePrint(str(_numChannel+1) + " :: " + millis());
        if (_numChannel < 8) {
          serial_openBCI.write((char)('0'+(_numChannel+1)));
        }
        if (_numChannel >= 8) {
          //openBCI.serial_openBCI.write((command_activate_channel_daisy[_numChannel-8]));
          serial_openBCI.write((command_activate_channel[_numChannel])); //command_activate_channel holds non-daisy and daisy
        }
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 2:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 3:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 4:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 5:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 6:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 7:
        verbosePrint(channelSettingValues[_numChannel][channelWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(channelSettingValues[_numChannel][channelWriteCounter-2]);
        //value for ON/OF
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 8:
        verbosePrint("X" + " :: " + millis());
        serial_openBCI.write('X'); // send 'X' to end message sequence
        timeOfLastChannelWrite = millis();
        channelWriteCounter++;
        break;
      case 9:
        //turn back off channels that were not active before changing channel settings
        switch(channelDeactivateCounter) {
        case 0:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 1:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 2:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 3:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 4:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 5:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 6:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 7:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          //check to see if it's 8chan or 16chan ... stop the switch case here if it's 8 chan, otherwise keep going
          if (nchan == 8) {
            verbosePrint("done writing channel.");
            isWritingChannel = false;
            channelWriteCounter = 0;
            channelDeactivateCounter = 0;
          } else {
            //keep going
          }
          break;
        case 8:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 9:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 10:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 11:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 12:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 13:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 14:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          channelDeactivateCounter++;
          break;
        case 15:
          if (channelSettingValues[channelDeactivateCounter][0] == '1') {
            verbosePrint("deactivating channel: " + str(channelDeactivateCounter + 1));
            serial_openBCI.write(command_deactivate_channel[channelDeactivateCounter]);
          }
          verbosePrint("done writing channel.");
          isWritingChannel = false;
          channelWriteCounter = 0;
          channelDeactivateCounter = 0;
          break;
        }

        // verbosePrint("done writing channel.");
        // isWritingChannel = false;
        // channelWriteCounter = -1;
        timeOfLastChannelWrite = millis();
        break;
      }
      // timeOfLastChannelWrite = millis();
      // channelWriteCounter++;
    }
  }

  private long timeOfLastImpWrite = 0;
  private int impWriteCounter = 0;
  private boolean isWritingImp = false;
  public boolean get_isWritingImp() {
    return isWritingImp;
  }
  public void initImpWrite(int _numChannel) {  //numChannel counts from zero
    timeOfLastImpWrite = millis();
    isWritingImp = true;
  }
  public void writeImpedanceSettings(int _numChannel, char[][] impedanceCheckValues) {  //numChannel counts from zero
    //after clicking an impedance button, write the new impedance settings for that channel to OpenBCI
    //after clicking any button, write the new settings for that channel to OpenBCI
    // verbosePrint("Writing impedance settings for channel " + _numChannel + " to OpenBCI!");
    //write setting 1, delay 5ms.. write setting 2, delay 5ms, etc.
    if (millis() - timeOfLastImpWrite >= 50) { //wait 50 milliseconds before sending next character
      verbosePrint("---");
      switch (impWriteCounter) {
      case 0: //start sequence by sending 'z'
        verbosePrint("z" + " :: " + millis());
        serial_openBCI.write('z');
        break;
      case 1: //send channel number
        verbosePrint(str(_numChannel+1) + " :: " + millis());
        if (_numChannel < 8) {
          serial_openBCI.write((char)('0'+(_numChannel+1)));
        }
        if (_numChannel >= 8) {
          //openBCI.serial_openBCI.write((command_activate_channel_daisy[_numChannel-8]));
          serial_openBCI.write((command_activate_channel[_numChannel])); //command_activate_channel holds non-daisy and daisy values
        }
        break;
      case 2:
      case 3:
        verbosePrint(impedanceCheckValues[_numChannel][impWriteCounter-2] + " :: " + millis());
        serial_openBCI.write(impedanceCheckValues[_numChannel][impWriteCounter-2]);
        //value for ON/OF
        break;
      case 4:
        verbosePrint("Z" + " :: " + millis());
        serial_openBCI.write('Z'); // send 'X' to end message sequence
        break;
      case 5:
        verbosePrint("done writing imp settings.");
        isWritingImp = false;
        impWriteCounter = -1;
        break;
      }
      timeOfLastImpWrite = millis();
      impWriteCounter++;
    }
  }
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  This file contains all key commands for interactivity with GUI & OpenBCI
//  Created by Chip Audette, Joel Murphy, & Conor Russomanno
//  - Extracted from DAC_GUI because it was getting too klunky
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//interpret a keypress...the key pressed comes in as "key"
public void keyPressed() {
  //note that the Processing variable "key" is the keypress as an ASCII character
  //note that the Processing variable "keyCode" is the keypress as a JAVA keycode.  This differs from ASCII
  //println(appName + ": keyPressed: key = " + key + ", int(key) = " + int(key) + ", keyCode = " + keyCode);

  if(!controlPanel.isOpen){ //don't parse the key if the control panel is open
    if ((PApplet.parseInt(key) >=32) && (PApplet.parseInt(key) <= 126)) {  //32 through 126 represent all the usual printable ASCII characters
      parseKey(key);
    } else {
      parseKeycode(keyCode);
    }
  }

  if(key==27){
    key=0; //disable 'esc' quitting program
  }
}

public void parseKey(char val) {
  int Ichan; boolean activate; int code_P_N_Both;

  //assumes that val is a usual printable ASCII character (ASCII 32 through 126)
  switch (val) {
    case ' ':
      stopButtonWasPressed();
      break;
    case '.':

      if(drawEMG){
        drawAccel = true;
        drawPulse = false;
        drawHead = false;
        drawEMG = false;
      }
      else if(drawAccel){
        drawAccel = false;
        drawPulse = true;
        drawHead = false;
        drawEMG = false;
      }
      else if(drawPulse){
        drawAccel = false;
        drawPulse = false;
        drawHead = true;
        drawEMG = false;
      }
      else if(drawHead){
        drawAccel = false;
        drawPulse = false;
        drawHead = false;
        drawEMG = true;
      }
      break;
    case ',':
      drawContainers = !drawContainers;
      break;
    case '<':
      w_timeSeries.setUpdating(!w_timeSeries.isUpdating());
      // drawTimeSeries = !drawTimeSeries;
      break;
    case '>':
      if(eegDataSource == DATASOURCE_GANGLION){
        ganglion.enterBootloaderMode();
      }
      break;
    case '{':
      if(colorScheme == COLOR_SCHEME_DEFAULT){
        colorScheme = COLOR_SCHEME_ALTERNATIVE_A;
      } else if(colorScheme == COLOR_SCHEME_ALTERNATIVE_A) {
        colorScheme = COLOR_SCHEME_DEFAULT;
      }
      topNav.updateNavButtonsBasedOnColorScheme();
      println("Changing color scheme.");
      break;
    case '/':
      drawAccel = !drawAccel;
      drawPulse = !drawPulse;
      break;
    case '\\':
      drawFFT = !drawFFT;
      drawBionics = !drawBionics;
      break;
    case '1':
      deactivateChannel(1-1);
      break;
    case '2':
      deactivateChannel(2-1);
      break;
    case '3':
      deactivateChannel(3-1);
      break;
    case '4':
      deactivateChannel(4-1);
      break;
    case '5':
      deactivateChannel(5-1);
      break;
    case '6':
      deactivateChannel(6-1);
      break;
    case '7':
      deactivateChannel(7-1);
      break;
    case '8':
      deactivateChannel(8-1);
      break;

    case 'q':
      if(nchan == 16){
        deactivateChannel(9-1);
      }
      break;
    case 'w':
      if(nchan == 16){
        deactivateChannel(10-1);
      }
      break;
    case 'e':
      if(nchan == 16){
        deactivateChannel(11-1);
      }
      break;
    case 'r':
      if(nchan == 16){
        deactivateChannel(12-1);
      }
      break;
    case 't':
      if(nchan == 16){
        deactivateChannel(13-1);
      }
      break;
    case 'y':
      if(nchan == 16){
        deactivateChannel(14-1);
      }
      break;
    case 'u':
      if(nchan == 16){
        deactivateChannel(15-1);
      }
      break;
    case 'i':
      if(nchan == 16){
        deactivateChannel(16-1);
      }
      break;

    //activate channels 1-8
    case '!':
      activateChannel(1-1);
      break;
    case '@':
      activateChannel(2-1);
      break;
    case '#':
      activateChannel(3-1);
      break;
    case '$':
      activateChannel(4-1);
      break;
    case '%':
      activateChannel(5-1);
      break;
    case '^':
      activateChannel(6-1);
      break;
    case '&':
      activateChannel(7-1);
      break;
    case '*':
      activateChannel(8-1);
      break;

    //activate channels 9-16 (DAISY MODE ONLY)
    case 'Q':
      if(nchan == 16){
        activateChannel(9-1);
      }
      break;
    case 'W':
      if(nchan == 16){
        activateChannel(10-1);
      }
      break;
    case 'E':
      if(nchan == 16){
        activateChannel(11-1);
      }
      break;
    case 'R':
      if(nchan == 16){
        activateChannel(12-1);
      }
      break;
    case 'T':
      if(nchan == 16){
        activateChannel(13-1);
      }
      break;
    case 'Y':
      if(nchan == 16){
        activateChannel(14-1);
      }
      break;
    case 'U':
      if(nchan == 16){
        activateChannel(15-1);
      }
      break;
    case 'I':
      if(nchan == 16){
        activateChannel(16-1);
      }
      break;

    //other controls
    case 's':
      println("case s...");
      stopRunning();
      // stopButtonWasPressed();
      break;
    case 'b':
      println("case b...");
      startRunning();
      // stopButtonWasPressed();
      break;
    case 'n':
      println("openBCI: " + openBCI);
      break;

    case '?':
      printRegisters();
      break;

    case 'd':
      verbosePrint("Updating GUI's channel settings to default...");
      // gui.cc.loadDefaultChannelSettings();
      w_timeSeries.hsc.loadDefaultChannelSettings();
      //openBCI.serial_openBCI.write('d');
      openBCI.configureAllChannelsToDefault();
      break;

    // //change the state of the impedance measurements...activate the N-channels
    // case 'A':
    //   Ichan = 1; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'S':
    //   Ichan = 2; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'D':
    //   Ichan = 3; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'F':
    //   Ichan = 4; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'G':
    //   Ichan = 5; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'H':
    //   Ichan = 6; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'J':
    //   Ichan = 7; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'K':
    //   Ichan = 8; activate = true; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;

    // //change the state of the impedance measurements...deactivate the N-channels
    // case 'Z':
    //   Ichan = 1; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'X':
    //   Ichan = 2; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'C':
    //   Ichan = 3; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'V':
    //   Ichan = 4; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'B':
    //   Ichan = 5; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'N':
    //   Ichan = 6; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case 'M':
    //   Ichan = 7; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;
    // case '<':
    //   Ichan = 8; activate = false; code_P_N_Both = 1;  setChannelImpedanceState(Ichan-1,activate,code_P_N_Both);
    //   break;


    case 'p':
     String picfname = "OpenBCI-" + getDateString() + ".jpg";
     println(appName + ": 'm' was pressed...taking screenshot:" + picfname);
     saveFrame("./SavedData/" + picfname);    // take a shot of that!
     break;

    default:
     println(appName + ": '" + key + "' Pressed...sending to OpenBCI...");
     // if (openBCI.serial_openBCI != null) openBCI.serial_openBCI.write(key);//send the value as ascii with a newline character
     //if (openBCI.serial_openBCI != null) openBCI.serial_openBCI.write(key);//send the value as ascii with a newline character
     openBCI.sendChar(key);

     break;
     
     case 'm':
       Marker_Trigger = "Marker Start";

     break;
     
  }
}

public void keyReleased() {
  if (key  == 'm') {
    Marker_Trigger = "Marker End";
  } else {
    Marker_Trigger = "";
  }
}


public void parseKeycode(int val) {
  //assumes that val is Java keyCode
  switch (val) {
    case 8:
      println(appName + ": parseKeycode(" + val + "): received BACKSPACE keypress.  Ignoring...");
      break;
    case 9:
      println(appName + ": parseKeycode(" + val + "): received TAB keypress.  Ignoring...");
      //gui.showImpedanceButtons = !gui.showImpedanceButtons;
      // gui.incrementGUIpage(); //deprecated with new channel controller
      break;
    case 10:
      println("Enter was pressed.");
      //drawPresentation = !drawPresentation;
      break;
    case 16:
      println(appName + ": parseKeycode(" + val + "): received SHIFT keypress.  Ignoring...");
      break;
    case 17:
      //println(appName + ": parseKeycode(" + val + "): received CTRL keypress.  Ignoring...");
      break;
    case 18:
      println(appName + ": parseKeycode(" + val + "): received ALT keypress.  Ignoring...");
      break;
    case 20:
      println(appName + ": parseKeycode(" + val + "): received CAPS LOCK keypress.  Ignoring...");
      break;
    case 27:
      println(appName + ": parseKeycode(" + val + "): received ESC keypress.  Stopping OpenBCI...");
      //stopRunning();
      break;
    case 33:
      println(appName + ": parseKeycode(" + val + "): received PAGE UP keypress.  Ignoring...");
      break;
    case 34:
      println(appName + ": parseKeycode(" + val + "): received PAGE DOWN keypress.  Ignoring...");
      break;
    case 35:
      println(appName + ": parseKeycode(" + val + "): received END keypress.  Ignoring...");
      break;
    case 36:
      println(appName + ": parseKeycode(" + val + "): received HOME keypress.  Ignoring...");
      break;
    case 37:
      //if (millis() - myPresentation.timeOfLastSlideChange >= 250) {
      //  if(myPresentation.currentSlide >= 0){
      //    myPresentation.slideBack();
      //    myPresentation.timeOfLastSlideChange = millis();
      //  }
      //}
      break;
    case 38:
      println(appName + ": parseKeycode(" + val + "): received UP ARROW keypress.  Ignoring...");
      dataProcessing_user.switchesActive = true;
      break;
    case 39:
      //if (millis() - myPresentation.timeOfLastSlideChange >= 250) {
      //  if(myPresentation.currentSlide < myPresentation.presentationSlides.length - 1){
      //    myPresentation.slideForward();
      //    myPresentation.timeOfLastSlideChange = millis();
      //  }
      //}
      //break;
    case 40:
      println(appName + ": parseKeycode(" + val + "): received DOWN ARROW keypress.  Ignoring...");
      dataProcessing_user.switchesActive = false;
      break;
    case 112:
      println(appName + ": parseKeycode(" + val + "): received F1 keypress.  Ignoring...");
      break;
    case 113:
      println(appName + ": parseKeycode(" + val + "): received F2 keypress.  Ignoring...");
      break;
    case 114:
      println(appName + ": parseKeycode(" + val + "): received F3 keypress.  Ignoring...");
      break;
    case 115:
      println(appName + ": parseKeycode(" + val + "): received F4 keypress.  Ignoring...");
      break;
    case 116:
      println(appName + ": parseKeycode(" + val + "): received F5 keypress.  Ignoring...");
      break;
    case 117:
      println(appName + ": parseKeycode(" + val + "): received F6 keypress.  Ignoring...");
      break;
    case 118:
      println(appName + ": parseKeycode(" + val + "): received F7 keypress.  Ignoring...");
      break;
    case 119:
      println(appName + ": parseKeycode(" + val + "): received F8 keypress.  Ignoring...");
      break;
    case 120:
      println(appName + ": parseKeycode(" + val + "): received F9 keypress.  Ignoring...");
      break;
    case 121:
      println(appName + ": parseKeycode(" + val + "): received F10 keypress.  Ignoring...");
      break;
    case 122:
      println(appName + ": parseKeycode(" + val + "): received F11 keypress.  Ignoring...");
      break;
    case 123:
      println(appName + ": parseKeycode(" + val + "): received F12 keypress.  Ignoring...");
      break;
    case 127:
      println(appName + ": parseKeycode(" + val + "): received DELETE keypress.  Ignoring...");
      break;
    case 155:
      println(appName + ": parseKeycode(" + val + "): received INSERT keypress.  Ignoring...");
      break;
    default:
      println(appName + ": parseKeycode(" + val + "): value is not known.  Ignoring...");
      break;
  }
}


//swtich yard if a click is detected
public void mousePressed() {

  verbosePrint(appName + ": mousePressed: mouse pressed");

  //if not before "Start System" ... i.e. after initial setup
  if (systemMode >= SYSTEMMODE_POSTINIT) {

    //limit interactivity of main GUI if control panel is open
    if (controlPanel.isOpen == false) {
      //was the stopButton pressed?

      // gui.mousePressed(); // trigger mousePressed function in GUI
      // GUIWidgets_mousePressed(); // to replace GUI_Manager version (above) soon... cdr 7/25/16
      wm.mousePressed();

      //check the graphs
      // if (gui.isMouseOnFFT(mouseX, mouseY)) {
      //   GraphDataPoint dataPoint = new GraphDataPoint();
      //   gui.getFFTdataPoint(mouseX, mouseY, dataPoint);
      //   println(appName + ": FFT data point: " + String.format("%4.2f", dataPoint.x) + " " + dataPoint.x_units + ", " + String.format("%4.2f", dataPoint.y) + " " + dataPoint.y_units);
      // } else if (gui.headPlot1.isPixelInsideHead(mouseX, mouseY)) {
      //   //toggle the head plot contours
      //   gui.headPlot1.drawHeadAsContours = !gui.headPlot1.drawHeadAsContours;
      // } else if (gui.isMouseOnMontage(mouseX, mouseY)) {
      //   //toggle the display of the montage values
      //   gui.showMontageValues  = !gui.showMontageValues;
      // }

      // if (gui.isMouseOnMontage(mouseX, mouseY)) {
      //   //toggle the display of the montage values
      //   gui.showMontageValues  = !gui.showMontageValues;
      // }
    }
  }

  //=============================//
  // CONTROL PANEL INTERACTIVITY //
  //=============================//

  // //was control panel button pushed
  // if (controlPanelCollapser.isMouseHere()) {
  //   if (controlPanelCollapser.isActive && systemMode == SYSTEMMODE_POSTINIT) {
  //     controlPanelCollapser.setIsActive(false);
  //     controlPanel.isOpen = false;
  //   } else {
  //     controlPanelCollapser.setIsActive(true);
  //     controlPanel.isOpen = true;
  //   }
  // } else {
  //   if (controlPanel.isOpen) {
  //     controlPanel.CPmousePressed();
  //   }
  // }

  //topNav is always clickable
  topNav.mousePressed();

  //interacting with control panel
  if (controlPanel.isOpen) {
    //close control panel if you click outside...
    if (systemMode == SYSTEMMODE_POSTINIT) {
      if (mouseX > 0 && mouseX < controlPanel.w && mouseY > 0 && mouseY < controlPanel.initBox.y+controlPanel.initBox.h) {
        println(appName + ": mousePressed: clicked in CP box");
        controlPanel.CPmousePressed();
      }
      //if clicked out of panel
      else {
        println(appName + ": mousePressed: outside of CP clicked");
        controlPanel.isOpen = false;
        topNav.controlPanelCollapser.setIsActive(false);
        output("Press the \"Press to Start\" button to initialize the data stream.");
      }
    }
  }

  redrawScreenNow = true;  //command a redraw of the GUI whenever the mouse is pressed

  if (playground.isMouseHere()) {
    playground.mousePressed();
  }

  if (playground.isMouseInButton()) {
    playground.toggleWindow();
  }


  //if (accelWidget.isMouseHere()) {
  //  accelWidget.mousePressed();
  //}

  //if (accelWidget.isMouseInButton()) {
  //  accelWidget.toggleWindow();
  //}

  //if (pulseWidget.isMouseHere()) {
  //  pulseWidget.mousePressed();
  //}

  //if (accelWidget.isMouseInButton()) {
  //  accelWidget.toggleWindow();
  //}

  //if (pulseWidget.isMouseHere()) {
  //  pulseWidget.mousePressed();
  //}

  //if (pulseWidget.isMouseInButton()) {
  //  pulseWidget.toggleWindow();
  //}
}

public void mouseReleased() {

  //some buttons light up only when being actively pressed.  Now that we've
  //released the mouse button, turn off those buttons.

  //interacting with control panel
  if (controlPanel.isOpen) {
    //if clicked in panel
    controlPanel.CPmouseReleased();
  }

  // gui.mouseReleased();
  topNav.mouseReleased();

  if (systemMode >= SYSTEMMODE_POSTINIT) {

    // GUIWidgets_mouseReleased(); // to replace GUI_Manager version (above) soon... cdr 7/25/16
    wm.mouseReleased();

    redrawScreenNow = true;  //command a redraw of the GUI whenever the mouse is released
  }

  if (screenHasBeenResized) {
    println(appName + ": mouseReleased: screen has been resized...");
    screenHasBeenResized = false;
  }

  //Playground Interactivity
  if (playground.isMouseHere()) {
    playground.mouseReleased();
  }
  if (playground.isMouseInButton()) {
    // playground.toggleWindow();
  }
}

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------


////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Formerly Button.pde
// This class creates and manages a button for use on the screen to trigger actions.
//
// Created: Chip Audette, Oct 2013.
// Modified: Conor Russomanno, Oct 2014
//
// Based on Processing's "Button" example code
//
////////////////////////////////////////////////////////////////////////////////////////////////////

class Button {

  int but_x, but_y, but_dx, but_dy;      // Position of square button
  //int rectSize = 90;     // Diameter of rect

  int currentColor;
  // color color_hover = color(127, 134, 143);//color(252, 221, 198);
  int color_hover = color(177, 184, 193);//color(252, 221, 198);
  int color_pressed = color(150,170,200); //bgColor;
  int color_highlight = color(102);
  int color_notPressed = color(255); //color(227,118,37);
  int buttonStrokeColor = bgColor;
  int textColorActive = color(255);
  int textColorNotActive = bgColor;
  int rectHighlight;
  boolean drawHand = false;
  boolean isCircleButton = false;
  int cornerRoundness = 0;
  //boolean isMouseHere = false;
  boolean buttonHasStroke = true;
  boolean isActive = false;
  boolean isDropdownButton = false;
  boolean wasPressed = false;
  public String but_txt;
  boolean showHelpText;
  boolean helpTimerStarted;
  String helpText= "";
  String myURL= "";
  int mouseOverButtonStart = 0;
  PFont buttonFont;
  int buttonTextSize;
  PImage bgImage;
  boolean hasbgImage = false;

  public Button(int x, int y, int w, int h, String txt) {
    setup(x, y, w, h, txt);
    buttonFont = p5;
    buttonTextSize = 12;
  }

  public Button(int x, int y, int w, int h, String txt, int fontSize) {
    setup(x, y, w, h, txt);
    buttonFont = p5;
    buttonTextSize = 12;
    //println(PFont.list()); //see which fonts are available
    //font = createFont("SansSerif.plain",fontSize);
    //font = createFont("Lucida Sans Regular",fontSize);
    // font = createFont("Arial",fontSize);
    //font = loadFont("SansSerif.plain.vlw");
  }

  public void setup(int x, int y, int w, int h, String txt) {
    but_x = x;
    but_y = y;
    but_dx = w;
    but_dy = h;
    setString(txt);
  }

  public void setX(int _but_x){
    but_x = _but_x;
  }

  public void setY(int _but_y){
    but_y = _but_y;
  }

  public void setPos(int _but_x, int _but_y){
    but_x = _but_x;
    but_y = _but_y;
  }

  public void setFont(PFont _newFont){
    buttonFont = _newFont;
  }

  public void setFont(PFont _newFont, int _newTextSize){
    buttonFont = _newFont;
    buttonTextSize = _newTextSize;
  }

  public void setCircleButton(boolean _isCircleButton){
    isCircleButton = _isCircleButton;
    if(isCircleButton){
      cornerRoundness = 0;
    }
  }

  public void setCornerRoundess(int _cornerRoundness){
    if(!isCircleButton){
      cornerRoundness = _cornerRoundness;
    }
  }

  public void setString(String txt) {
    but_txt = txt;
    //println("Button: setString: string = " + txt);
  }

  public void setHelpText(String _helpText){
    helpText = _helpText;
  }

  public void setURL(String _myURL){
    myURL = _myURL;
  }

  public void goToURL(){
    if(myURL != ""){
      openURLInBrowser(myURL);
    }
  }

  public void setBackgroundImage(PImage _bgImage){
    bgImage = _bgImage;
    hasbgImage = true;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setIsActive(boolean val) {
    isActive = val;
  }

  public void makeDropdownButton(boolean val) {
    isDropdownButton = val;
  }

  public boolean isMouseHere() {
    if ( overRect(but_x, but_y, but_dx, but_dy) ) {
      // cursor(HAND);
      if(!helpTimerStarted){
        helpTimerStarted = true;
        mouseOverButtonStart = millis();
      } else {
        if(millis()-mouseOverButtonStart >= 1000){
          showHelpText = true;
        }
      }
      return true;
    }
    else {
      setIsActive(false);
      if(helpTimerStarted){
        buttonHelpText.setVisible(false);
        showHelpText = false;
        helpTimerStarted = false;
      }
      return false;
    }
  }

  public int getColor() {
    if (isActive) {
     currentColor = color_pressed;
    } else if (isMouseHere()) {
     currentColor = color_hover;
    } else {
     currentColor = color_notPressed;
    }
    return currentColor;
  }

  public void setCurrentColor(int _color){
    currentColor = _color;
  }

  public void setColorPressed(int _color) {
    color_pressed = _color;
  }
  public void setColorNotPressed(int _color) {
    color_notPressed = _color;
  }

  public void setStrokeColor(int _color) {
    buttonStrokeColor = _color;
  }

  public void hasStroke(boolean _trueORfalse) {
    buttonHasStroke = _trueORfalse;
  }

  public boolean overRect(int x, int y, int width, int height) {
    if (mouseX >= x && mouseX <= x+width &&
      mouseY >= y && mouseY <= y+height) {
      return true;
    } else {
      return false;
    }
  }

  public void draw(int _x, int _y) {
    but_x = _x;
    but_y = _y;
    draw();
  }

  public void draw() {
    pushStyle();
    // rectMode(CENTER);
    ellipseMode(CORNER);

    //draw the button
    fill(getColor());
    if (buttonHasStroke) {
      stroke(buttonStrokeColor); //button border
    } else {
      noStroke();
    }
    // noStroke();
    if(isCircleButton){
      ellipse(but_x, but_y, but_dx, but_dy); //draw circular button
    } else{
      if(cornerRoundness == 0){
        rect(but_x, but_y, but_dx, but_dy); //draw normal rectangle button
      } else {
        rect(but_x, but_y, but_dx, but_dy, cornerRoundness); //draw button with rounded corners
      }
    }

    //draw the text
    if (isActive) {
      fill(textColorActive);
    } else {
      fill(textColorNotActive);
    }
    stroke(255);
    textFont(buttonFont);  //load f2 ... from control panel
    textSize(buttonTextSize);
    textAlign(CENTER, CENTER);
    textLeading(round(0.9f*(textAscent()+textDescent())));
    //    int x1 = but_x+but_dx/2;
    //    int y1 = but_y+but_dy/2;
    int x1, y1;
    //no auto wrap
    x1 = but_x+but_dx/2;
    y1 = but_y+but_dy/2;

    if(hasbgImage){ //if there is a bg image ... don't draw text
      imageMode(CENTER);
      image(bgImage, but_x + (but_dx/2), but_y + (but_dy/2), but_dx-8, but_dy-8);
    } else{  //otherwise draw text
      if(buttonFont == h1 || buttonFont == h2 || buttonFont == h3 || buttonFont == h4 || buttonFont == h5){
        text(but_txt, x1, y1 - 1); //for some reason y looks better at -1 with montserrat
      } else if(buttonFont == p1 || buttonFont == p2 || buttonFont == p3 || buttonFont == p4 || buttonFont == p5 || buttonFont == p6){
        textLeading(12); //line spacing
        text(but_txt, x1, y1 - 2); //for some reason y looks better at -2 w/ Open Sans
      } else{
        text(but_txt, x1, y1); //as long as font is not Montserrat
      }
    }

    //send some info to the HelpButtonText object to be drawn last in DAC_GUI.pde ... we want to make sure it is render last, and on top of all other GUI stuff
    if(showHelpText && helpText != ""){
      buttonHelpText.setButtonHelpText(helpText, but_x + but_dx/2, but_y + (3*but_dy)/4);
      buttonHelpText.setVisible(true);
    }
    //draw open/close arrow if it's a dropdown button
    if (isDropdownButton) {
      pushStyle();
      fill(255);
      noStroke();
      // smooth();
      // stroke(255);
      // strokeWeight(1);
      if (isActive) {
        float point1x = but_x + (but_dx - ((3f*but_dy)/4f));
        float point1y = but_y + but_dy/3f;
        float point2x = but_x + (but_dx-(but_dy/4f));
        float point2y = but_y + but_dy/3f;
        float point3x = but_x + (but_dx - (but_dy/2f));
        float point3y = but_y + (2f*but_dy)/3f;
        triangle(point1x, point1y, point2x, point2y, point3x, point3y); //downward triangle, indicating open
      } else {
        float point1x = but_x + (but_dx - ((3f*but_dy)/4f));
        float point1y = but_y + (2f*but_dy)/3f;
        float point2x = but_x + (but_dx-(but_dy/4f));
        float point2y = but_y + (2f*but_dy)/3f;
        float point3x = but_x + (but_dx - (but_dy/2f));
        float point3y = but_y + but_dy/3f;
        triangle(point1x, point1y, point2x, point2y, point3x, point3y); //upward triangle, indicating closed
      }
      popStyle();
    }

    //cursor = funny looking finger thing when hovering over buttons...
    // if (true) {
    //   if (!isMouseHere() && drawHand) {
    //     cursor(ARROW);
    //     drawHand = false;
    //     //verbosePrint("don't draw hand");
    //   }
    //   //if cursor is over button change cursor icon to hand!
    //   if (isMouseHere() && !drawHand) {
    //     cursor(HAND);
    //     drawHand = true;
    //     //verbosePrint("draw hand");
    //   }
    // }

    popStyle();
  } //end of button draw
};

class ButtonHelpText{
  int x, y, w, h;
  String myText = "";
  boolean isVisible;
  int numLines;
  int lineSpacing = 14;
  int padding = 10;

  ButtonHelpText(){

  }

  public void setVisible(boolean _isVisible){
    isVisible = _isVisible;
  }

  public void setButtonHelpText(String _myText, int _x, int _y){
    myText = _myText;
    x = _x;
    y = _y;
  }

  public void draw(){
    // println("4");
    if(isVisible){
      pushStyle();
      textAlign(CENTER, TOP);

      textFont(p5,12);
      textLeading(lineSpacing); //line spacing
      stroke(31,69,110);
      fill(255);
      numLines = (int)((float)myText.length()/30.0f) + 1; //add 1 to round up
      // println("numLines: " + numLines);
      //if on left side of screen, draw box brightness to prevent box off screen
      if(x <= width/2){
        rect(x, y, 200, 2*padding + numLines*lineSpacing + 4);
        fill(31,69,110); //text colof
        text(myText, x + padding, y + padding, 180, (numLines*lineSpacing + 4));
      } else{ //if on right side of screen, draw box left to prevent box off screen
        rect(x - 200, y, 200, 2*padding + numLines*lineSpacing + 4);
        fill(31,69,110); //text colof
        text(myText, x + padding - 200, y + padding, 180, (numLines*lineSpacing + 4));
      }
      popStyle();
    }
  }
};

public void openURLInBrowser(String _url){
  try {
    //Set your page url in this string. For eg, I m using URL for Google Search engine
    String url = _url;
    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
    output("Attempting to use your default browser to launch: " + url);
  }
  catch (java.io.IOException e) {
      System.out.println(e.getMessage());
  }
}
/////////////////////////////////////////////////////////////////////////////////
//
//  Emg_Widget is used to visiualze EMG data by channel, and to trip events
//
//  Created: Colin Fausnaught, December 2016 (with a lot of reworked code from Tao)
//
//  Custom widget to visiualze EMG data. Features dragable thresholds, serial
//  out communication, channel configuration, digital and analog events.
//
//  KNOWN ISSUES: Cannot resize with window dragging events
//
//  TODO: Add dynamic threshold functionality
////////////////////////////////////////////////////////////////////////////////

// addDropdown("SmoothEMG", "Smooth", Arrays.asList("0.01 s", "0.1 s", "0.15 s", "0.25 s", "0.5 s", "1.0 s", "2.0 s"), 0);
// addDropdown("uVLimit", "uV Limit", Arrays.asList("50 uV", "100 uV", "200 uV", "400 uV"), 0);
// addDropdown("CreepSpeed", "Creep", Arrays.asList("0.9", "0.95", "0.98", "0.99", "0.999"), 0);
// addDropdown("minUVRange", "Min \u0394uV", Arrays.asList("10 uV", "20 uV", "40 uV", "80 uV"), 0);
//
// int averagePeriod = 125;          //number of data packets to average over (250 = 1 sec)
// float acceptableLimitUV = 200.0;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
// float creepSpeed = 0.99;
// float minRange = 20.0;

public void MatrixSmoothEMG(int n){

  float samplesPerSecond;
  if(eegDataSource == DATASOURCE_GANGLION){
    samplesPerSecond = 200;
  } else {
    samplesPerSecond = 250;
  }

  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.01f;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.1f;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.15f;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.25f;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.5f;
    }
    if(n == 5){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.75f;
    }
    if(n == 6){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 1.0f;
    }
    if(n == 7){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 2.0f;
    }
  }
  closeAllDropdowns();
}

public void MatrixuVLimit(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].acceptableLimitUV = 50.0f;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].acceptableLimitUV = 100.0f;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].acceptableLimitUV = 200.0f;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].acceptableLimitUV = 400.0f;
    }
  }
  closeAllDropdowns();
}

public void MatrixCreepSpeed(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].creepSpeed = 0.9f;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].creepSpeed = 0.95f;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].creepSpeed = 0.98f;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].creepSpeed = 0.99f;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].creepSpeed = 0.999f;
    }
  }
  closeAllDropdowns();
}

public void MatrixminUVRange(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].minRange = 10.0f;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].minRange = 20.0f;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].minRange = 40.0f;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].minRange = 80.0f;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].minRange = 5.0f;
    }
    if(n == 5){
      w_matrix.motorWidgets[i].minRange = 1.0f;
    }
    if(n == 6){
      w_matrix.motorWidgets[i].minRange = 0.05f;
    }
    if(n == 7){
      w_matrix.motorWidgets[i].minRange = 18.0f;
    }
    if(n == 8){
      w_matrix.motorWidgets[i].minRange = 15.0f;
    }
    if(n == 9){
      w_matrix.motorWidgets[i].minRange = 30.0f;
    }
    if(n == 10){
      w_matrix.motorWidgets[i].minRange = 25.0f;
    }
  }
  closeAllDropdowns();
}

class W_matrix extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  Motor_Widget[] motorWidgets;
  TripSlider[] tripSliders;
  TripSlider[] untripSliders;
  List<String> baudList;
  List<String> serList;
  List<String> channelList;
  boolean[] events;
  int currChannel;
  int theBaud;
  Button connectButton;
  Button soundButton;
  Serial serialOutMatrix;
  String theSerial;
  
  char startMarker = 60;
  char endMarker = 62;
  char serialDelimiter = ',';
  
  boolean[] serialChannels = new boolean[16];
  boolean speaking = false;
  
  Boolean MatrixAdvanced = true;

  PApplet parent;

  W_matrix (PApplet _parent) {
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
    parent = _parent;



    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function

    //use these as new configuration widget
    motorWidgets = new Motor_Widget[nchan];

    for (int i = 0; i < nchan; i++) {
      motorWidgets[i] = new Motor_Widget();
      motorWidgets[i].ourChan = i;
      if(eegDataSource == DATASOURCE_GANGLION){
        motorWidgets[i].averagePeriod = 200 * 0.5f;
      } else {
        motorWidgets[i].averagePeriod = 250 * 0.5f;
      }
    }

    events = new boolean[nchan];

    for (int i = 0; i < nchan; i++) {
      events[i] = true;
    }

    addDropdown("MatrixSmoothEMG", "Smooth", Arrays.asList("0.01 s", "0.1 s", "0.15 s", "0.25 s", "0.5 s", "0.75 s", "1.0 s", "2.0 s"), 4);
    addDropdown("MatrixuVLimit", "uV Limit", Arrays.asList("50 uV", "100 uV", "200 uV", "400 uV"), 2);
    addDropdown("MatrixCreepSpeed", "Creep", Arrays.asList("0.9", "0.95", "0.98", "0.99", "0.999"), 3);
    addDropdown("MatrixminUVRange", "Min \u0394uV", Arrays.asList("10 uV", "20 uV", "40 uV", "80 uV", "5 uV" ,"1 uV", ".05", "18", "15", "30","25"), 1);

    if (MatrixAdvanced) {
      channelList = new ArrayList<String>();
      baudList = new ArrayList<String>();
      serList = new ArrayList<String>();
      for (int i = 0; i < nchan; i++) {
        channelList.add(Integer.toString(i + 1));
      }

      currChannel = 0;
      theBaud = 230400;

      baudList.add("NONE");
      baudList.add(Integer.toString(230400));
      baudList.add(Integer.toString(115200));
      baudList.add(Integer.toString(57600));
      baudList.add(Integer.toString(38400));
      baudList.add(Integer.toString(28800));
      baudList.add(Integer.toString(19200));
      baudList.add(Integer.toString(14400));
      baudList.add(Integer.toString(9600));
      baudList.add(Integer.toString(7200));
      baudList.add(Integer.toString(4800));
      baudList.add(Integer.toString(3600));
      // // ignore below here... I don't think these baud rates will be necessary
      // baudList.add(Integer.toString(2400));
      // baudList.add(Integer.toString(1800));
      // baudList.add(Integer.toString(1200));
      // baudList.add(Integer.toString(600));
      // baudList.add(Integer.toString(300));

      String[] serialPorts = Serial.list();
      serList.add("NONE");
      for (int i = 0; i < serialPorts.length; i++) {
        String tempPort = serialPorts[(serialPorts.length - 1) - i];
        if (!tempPort.equals(openBCI_portName)) serList.add(tempPort);
      }

      addDropdown("MatrixSerialSelection", "Port", serList, 0);
      //addDropdown("ChannelSelection", "Channel", channelList, 0);
      //addDropdown("EventType", "Event Type", Arrays.asList("Digital", "Analog"), 0);
      addDropdown("MatrixBaudRate", "Baud Rate", baudList, 0);
      tripSliders = new TripSlider[nchan];
      untripSliders = new TripSlider[nchan];

      initSliders(w, h);
    }
  }

  //Initalizes the threshold
  public void initSliders(int rw, int rh) {
    //Stole some logic from the rectangle drawing in draw()
    int rowNum = 4;
    int colNum = motorWidgets.length / rowNum;
    int index = 0;

    float rowOffset = rh / rowNum;
    float colOffset = rw / colNum;

    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {

        println("ROW: " + (4*rowOffset/8));
        tripSliders[index] = new TripSlider(PApplet.parseInt((5*colOffset/8) * 0.498f), PApplet.parseInt((2 * rowOffset / 8) * 0.384f), (4*rowOffset/8) * 0.408f, PApplet.parseInt((3*colOffset/32) * 0.489f), 2, tripSliders, true, motorWidgets[index]);
        untripSliders[index] = new TripSlider(PApplet.parseInt((5*colOffset/8) * 0.498f), PApplet.parseInt((2 * rowOffset / 8) * 0.384f), (4*rowOffset/8) * 0.408f, PApplet.parseInt((3*colOffset/32) * 0.489f), 2, tripSliders, false, motorWidgets[index]);
        //println("Slider :" + (j+i) + " first: " + int((5*colOffset/8) * 0.498)+ " second: " + int((2 * rowOffset / 8) * 0.384) + " third: " + int((3*colOffset/32) * 0.489));
        tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
        untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
        index++;
      }
    }
  }

  public void update() {
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff);
    //processTripps();
  }

  public void draw() {
    
    
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();
    noStroke();
    fill(255);
    rect(x, y, w, h);

    if (MatrixAdvanced) {
      if (connectButton != null) connectButton.draw();
      else connectButton = new Button(PApplet.parseInt(x) + 2, PApplet.parseInt(y) - navHeight + 2, 35, navHeight - 6, "Conn", fontInfo.buttonLabel_size);
      if (soundButton != null) soundButton.draw();
      else soundButton = new Button(PApplet.parseInt(x) + 60, PApplet.parseInt(y) - navHeight + 2, 50, navHeight - 6, "Voice", fontInfo.buttonLabel_size);

      stroke(1, 18, 41, 125);

      if (connectButton != null && connectButton.wasPressed) {
        fill(0, 255, 0);
        ellipse(x + 50, y - navHeight/2, 8, 16);
      } else if (connectButton != null && !connectButton.wasPressed) {
        fill(255, 0, 0);
        ellipse(x + 50, y - navHeight/2, 8, 16);
      }
        
    }


    // float rx = x, ry = y + 2* navHeight, rw = w, rh = h - 2*navHeight;
    float rx = x, ry = y, rw = w, rh = h;
    float scaleFactor = 1.0f;
    float scaleFactorJaw = 1.5f;
    int rowNum = 4;
    int colNum = motorWidgets.length / rowNum;
    float rowOffset = rh / rowNum;
    float colOffset = rw / colNum;
    int index = 0;
    float currx, curry;
    //new
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        
        if (isRunning){
          float outputVal = motorWidgets[i * colNum + j].myAverage;
            int tester = i * colNum + j +1;
            //println(tester + ": " + outputVal);
            
            //println( serialChannels);
        }
        
        //%%%%%
        pushMatrix();
        currx = rx + j * colOffset;
        curry = ry + i * rowOffset; //never name variables on an empty stomach
        translate(currx, curry);

        //draw visualizer

        // (int)color(129, 129, 129),
        // (int)color(124, 75, 141),
        // (int)color(54, 87, 158),
        // (int)color(49, 113, 89),
        // (int)color(221, 178, 13),
        // (int)color(253, 94, 52),
        // (int)color(224, 56, 45),
        // (int)color(162, 82, 49),

        //realtime
        // fill(255, 0, 0, 125);
        
        
        //circles
        
        //fill(red(channelColors[index%8]), green(channelColors[index%8]), blue(channelColors[index%8]), 200);
        //noStroke();
        //ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].myAverage, scaleFactor * motorWidgets[i * colNum + j].myAverage);
        
        ////circle for outer threshold
        //// stroke(0, 255, 0);
        //noFill();
        //strokeWeight(1);
        //stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        //ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].upperThreshold, scaleFactor * motorWidgets[i * colNum + j].upperThreshold);

        ////circle for inner threshold
        //// stroke(0, 255, 255);
        //stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        //ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold);
        
        // end Circles

        int _x = PApplet.parseInt(3*colOffset/8);
        int _y = PApplet.parseInt(2 * rowOffset / 8);
        int _w = PApplet.parseInt(5*colOffset/32);
        int _h = PApplet.parseInt(4*rowOffset/8);

        //draw normalized bar graph of uV w/ matching channel color
        noStroke();
        fill(red(channelColors[index%8]), green(channelColors[index%8]), blue(channelColors[index%8]), 200);
        rect(_x, 3*_y + 1, _w, map(motorWidgets[i * colNum + j].output_normalized, 0, 1, 0, (-1) * PApplet.parseInt((4*rowOffset/8))));

        //draw background bar container for mapped uV value indication
        strokeWeight(1);
        stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        noFill();
        rect(_x, _y, _w, _h);

        //draw trip & untrip threshold bars
        if (MatrixAdvanced) {
          tripSliders[index].update(currx, curry);
          tripSliders[index].display(_x, _y, _w, _h);
          untripSliders[index].update(currx, curry);
          untripSliders[index].display(_x, _y, _w, _h);
        }

        //draw channel number at upper left corner of row/column cell
        pushStyle();
        stroke(0);
        fill(bgColor);
        int _chan = index+1;
        textFont(p5, 12);
        text(_chan + "", 10, 20);
        // rectMode(CORNERS);
        // rect(0, 0, 10, 10);
        popStyle();

        index++;
        popMatrix();
      }
    }

    popStyle();
    if (watchBeat == true){
      if ( beat.isKick() ) {
        serialChannels[12] = true;
        serialChannels[4] = true;
        serialChannels[14] = true;
        
      } else {
        serialChannels[12] = false;
        serialChannels[4] = false;
        serialChannels[14] = false;
      }
      if ( beat.isHat() ) {
        serialChannels[3] = true;
        
      } else {
        serialChannels[3] = false;
      }
      if ( beat.isSnare() ) {
        serialChannels[1] = true;
        serialChannels[2] = true;
        serialChannels[0] = true;
        
      } else {
        serialChannels[1] = false;
        serialChannels[2] = false;
        serialChannels[0] = false;
      }
      sendSerial();
    }
  }

  public void screenResized() {
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    //widgetTemplateButton.setPos(x + w/2 - widgetTemplateButton.but_dx/2, y + h/2 - widgetTemplateButton.but_dy/2);
    if (MatrixAdvanced) {
      //connectButton.setPos(int(x) + 2, int(y) - navHeight + 2);

      for (int i = 0; i < tripSliders.length; i++) {
        //update slider positions
      }
    }
  }

  public void mousePressed() {
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (MatrixAdvanced) {
      if (connectButton.isMouseHere()) {
        connectButton.setIsActive(true);
        println("Connect pressed");
      } else connectButton.setIsActive(false);
      if (soundButton.isMouseHere()){
        speaking = !speaking;
        
      }
    }
  }

  public void mouseReleased() {
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    if (MatrixAdvanced) {
      if (connectButton != null && connectButton.isMouseHere()) {
        //do some function

        try {
          serialOutMatrix = new Serial(parent, theSerial, 230400);
          connectButton.wasPressed = true;
          verbosePrint("Connected");
          output("Connected to " + theSerial);
        }
        catch (Exception e) {
          connectButton.wasPressed = false;
          verbosePrint("Could not connect!");
          println(e);
          output("Could not connect. Confirm that your Serial/COM port is correct and active.");
          if (serialOutMatrix != null) { 
            serialOutMatrix.stop();
          }
        }

        connectButton.setIsActive(false);
      }

      for (int i = 0; i<nchan; i++) {
        tripSliders[i].releaseEvent();
        untripSliders[i].releaseEvent();
      }
    }
  }


  public void process(float[][] data_newest_uV, //holds raw EEG data that is new since the last call
    float[][] data_long_uV, //holds a longer piece of buffered EEG data, of same length as will be plotted on the screen
    float[][] data_forDisplay_uV, //this data has been filtered and is ready for plotting on the screen
    FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data
    
    //for example, you could loop over each EEG channel to do some sort of time-domain processing
    //using the sample values that have already been filtered, as will be plotted on the display
    //float EEG_value_uV;

    //looping over channels and analyzing input data
    for (Motor_Widget cfc : motorWidgets) {
      cfc.myAverage = 0.0f;
      for (int i = data_forDisplay_uV[cfc.ourChan].length - PApplet.parseInt(cfc.averagePeriod); i < data_forDisplay_uV[cfc.ourChan].length; i++) {
        if (abs(data_forDisplay_uV[cfc.ourChan][i]) <= cfc.acceptableLimitUV) { //prevent BIG spikes from effecting the average
          cfc.myAverage += abs(data_forDisplay_uV[cfc.ourChan][i]);  //add value to average ... we will soon divide by # of packets
        } else {
          cfc.myAverage += cfc.acceptableLimitUV; //if it's greater than the limit, just add the limit
        }
      }
      cfc.myAverage = cfc.myAverage / cfc.averagePeriod; // float(cfc.averagePeriod); //finishing the average

      if (cfc.myAverage >= cfc.upperThreshold && cfc.myAverage <= cfc.acceptableLimitUV) { //
        cfc.upperThreshold = cfc.myAverage;
      }
      if (cfc.myAverage <= cfc.lowerThreshold) {
        cfc.lowerThreshold = cfc.myAverage;
      }
      if (cfc.upperThreshold >= (cfc.myAverage + cfc.minRange)) {  //minRange = 15
        cfc.upperThreshold *= cfc.creepSpeed; //adjustmentSpeed
      }
      if (cfc.lowerThreshold <= 1){
        cfc.lowerThreshold = 1.0f;
      }
      if (cfc.lowerThreshold <= cfc.myAverage) {
        cfc.lowerThreshold *= (1)/(cfc.creepSpeed); //adjustmentSpeed
        // cfc.lowerThreshold += (10 - cfc.lowerThreshold)/(frameRate * 5); //have lower threshold creep upwards to keep range tight
      }
      if (cfc.upperThreshold <= (cfc.lowerThreshold + cfc.minRange)){
        cfc.upperThreshold = cfc.lowerThreshold + cfc.minRange;
      }
      // if (cfc.upperThreshold >= (cfc.myAverage + 35)) {
      //   cfc.upperThreshold *= .97;
      // }
      // if (cfc.lowerThreshold <= cfc.myAverage) {
      //   cfc.lowerThreshold += (10 - cfc.lowerThreshold)/(frameRate * 5); //have lower threshold creep upwards to keep range tight
      // }
      //output_L = (int)map(myAverage_L, lowerThreshold_L, upperThreshold_L, 0, 255);
      cfc.output_normalized = map(cfc.myAverage, cfc.lowerThreshold, cfc.upperThreshold, 0, 1);
      if(cfc.output_normalized < 0){
        cfc.output_normalized = 0; //always make sure this value is >= 0
      }
      cfc.output_adjusted = ((-0.1f/(cfc.output_normalized*255.0f)) + 255.0f);



      //=============== TRIPPIN ==================
      //= Just calls all the trip events         =
      //==========================================

      switch(cfc.ourChan) {

      case 0:
        if (events[0]) digitalEventChanHander(0,cfc);
        else analogEventChan0(cfc);
        break;
      case 1:
        if (events[1]) digitalEventChanHander(1,cfc);
        else analogEventChan1(cfc);
        break;
      case 2:
        if (events[2]) digitalEventChanHander(2,cfc);
        else analogEventChan2(cfc);
        break;
      case 3:
        if (events[3]) digitalEventChanHander(3,cfc);
        else analogEventChan3(cfc);
        break;
      case 4:
        if (events[4]) digitalEventChanHander(4,cfc);
        else analogEventChan4(cfc);
        break;
      case 5:
        if (events[5]) digitalEventChanHander(5,cfc);
        else  analogEventChan5(cfc);
        break;
      case 6:
        if (events[6]) digitalEventChanHander(6,cfc);
        else analogEventChan6(cfc);
        break;
      case 7:
        if (events[7]) digitalEventChanHander(7,cfc);
        else analogEventChan7(cfc);
        break;
      case 8:
        if (events[8]) digitalEventChanHander(8,cfc);
        else analogEventChan8(cfc);
        break;
      case 9:
        if (events[9]) digitalEventChanHander(9,cfc);
        else analogEventChan9(cfc);
        break;
      case 10:
        if (events[10]) digitalEventChanHander(10,cfc);
        else analogEventChan10(cfc);
        break;
      case 11:
        if (events[11]) digitalEventChanHander(11,cfc);
        else analogEventChan11(cfc);
        break;
      case 12:
        if (events[12]) digitalEventChanHander(12,cfc);
        else analogEventChan12(cfc);
        break;
      case 13:
        if (events[13]) digitalEventChanHander(13,cfc);
        else analogEventChan13(cfc);
        break;
      case 14:
        if (events[14]) digitalEventChanHander(14,cfc);
        else analogEventChan14(cfc);
        break;
      case 15:
        if (events[15]) digitalEventChanHander(15,cfc);
        else analogEventChan15(cfc);
        break;
      default:
        break;
      }
     
    }
    //=================== OpenBionics switch example ==============================

    if (millis() - motorWidgets[0].timeOfLastTrip >= 2000 && serialOutMatrix != null) {
      //println("Counter: " + motorWidgets[0].switchCounter);
      switch(motorWidgets[0].switchCounter) {
      case 1:
        //serialOutMatrix.write("G0");
       println("OVER");
        break;
      }
      motorWidgets[0].switchCounter = 0;
    }

    //----------------- Leftover from Tou Code, what does this do? ----------------------------
    //OR, you could loop over each EEG channel and do some sort of frequency-domain processing from the FFT data
    //float FFT_freq_Hz, FFT_value_uV;
    //for (int Ichan=0; Ichan < nchan; Ichan++) {
    //  //loop over each new sample
    //  for (int Ibin=0; Ibin < fftBuff[Ichan].specSize(); Ibin++) {
    //    FFT_freq_Hz = fftData[Ichan].indexToFreq(Ibin);
    //    FFT_value_uV = fftData[Ichan].getBand(Ibin);

    //    println(Ichan + " : " + FFT_value_uV);
    //  }
    //}
    //---------------------------------------------------------------------------------
  }
  public void processTripps(){
    if (speaking == true){
      if (serialChannels[0] == true){
        if (serialChannels[1] == true){
        } else {
          playVoice(0);
        }
      }
     if (serialChannels[1] == true) {
        if (serialChannels[0] == true){
        } else {
          playVoice(1);
        }
       
     }
     if (serialChannels[12] == true) {
        playVoice(12);
     }
      if (serialChannels[13] == true) {
        playVoice(13);
     }
    }
    
  }

  class Motor_Widget {
    //variables
    boolean isTriggered = false;
    float upperThreshold = 25;        //default uV upper threshold value ... this will automatically change over time
    float lowerThreshold = 0;         //default uV lower threshold value ... this will automatically change over time
    int thresholdPeriod = 1250;       //number of packets
    int ourChan = 0;                  //channel being monitored ... "3 - 1" means channel 3 (with a 0 index)
    float myAverage = 0.0f;            //this will change over time ... used for calculations below
    //prez related
    boolean switchTripped = false;
    int switchCounter = 0;
    float timeOfLastTrip = 0;
    float timeOfLastOff = 0;
    float tripThreshold = 0.75f;
    float untripThreshold = 0.5f;
    //if writing to a serial port
    int output = 0;                   //value between 0-255 that is the relative position of the current uV average between the rolling lower and upper uV thresholds
    float output_normalized = 0;      //converted to between 0-1
    float output_adjusted = 0;        //adjusted depending on range that is expected on the other end, ie 0-255?
    boolean analogBool = true;        //Analog events?
    boolean digitalBool = true;       //Digital events?

    //these are the 4 variables affected by the dropdown menus
    float averagePeriod; // = 125;          //number of data packets to average over (250 = 1 sec)
    float acceptableLimitUV = 200.0f;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
    float creepSpeed = 0.99f;
    float minRange = 20.0f;

  };

  //============= TripSlider =============
  //=  Class for moving thresholds. Can  =
  //=  be dragged up and down, but lower =
  //=  thresholds cannot go above upper  =
  //=  thresholds (and visa versa).      =
  //======================================
  class TripSlider {
    //Fields
    int lx, ly;
    int boxx, boxy;
    int stretch;
    int wid;
    int len;
    int boxLen;
    boolean over;
    boolean press;
    boolean locked = false;
    boolean otherslocked = false;
    boolean trip;
    boolean drawHand;
    TripSlider[] others;
    int current_color = color(255, 255, 255);
    Motor_Widget parent;

    //Constructor
    TripSlider(int ix, int iy, float il, int iwid, int ilen, TripSlider[] o, boolean wastrip, Motor_Widget p) {
      lx = ix;
      ly = iy;
      boxLen = PApplet.parseInt(il);
      wid = iwid;
      len = ilen;
      boxx = lx - wid/2;
      //boxx = lx;
      boxy = ly-stretch - len/2;
      //boxy = ly;
      others = o;
      trip = wastrip;  //Boolean to distinguish between trip and untrip thresholds
      parent = p;
      //boxLen = 31;
    }

    //Called whenever thresholds are dragged
    public void update(float tx, float ty) {
      // println("testing...");
      boxx = lx;
      //boxy = (wid + (ly/2)) - int(((wid + (ly/2)) - ly) * (float(stretch) / float(wid)));
      //boxy = ly + (ly - int( ly * (float(stretch) / float(wid)))) ;
      boxy = PApplet.parseInt(ly + stretch); //- stretch;

      for (int i=0; i<others.length; i++) {
        if (others[i].locked == true) {
          otherslocked = true;
          break;
        } else {
          otherslocked = false;
        }
      }

      if (otherslocked == false) {
        overEvent(tx, ty);
        pressEvent();
      }

      if (press) {
        //Some of this may need to be refactored in order to support window resizing
        // int mappedVal = int(mouseY - (ty + ly));
        // //int mappedVal = int(map((mouseY - (ty + ly) ), ((ty+ly) + wid - (ly/2)) - (ty+ly), 0, 0, wid));
        int mappedVal = PApplet.parseInt(mouseY - (ty+ly));

        //println("bxLen: " + boxLen + " ty: " + ty + " ly: " + ly + " mouseY: " + mouseY + " boxy: " + boxy + " stretch: " + stretch + " width: " + wid + " mappedVal: " + mappedVal);

        if (!trip) stretch = lock(mappedVal, PApplet.parseInt(parent.untripThreshold * (boxLen)), boxLen);
        else stretch =  lock(mappedVal, 0, PApplet.parseInt(parent.tripThreshold * (boxLen)));

        if (mappedVal > boxLen && !trip) parent.tripThreshold = 1;
        else if (mappedVal > boxLen && trip) parent.untripThreshold = 1;
        else if (mappedVal < 0 && !trip) parent.tripThreshold = 0;
        else if (mappedVal < 0 && trip) parent.untripThreshold = 0;
        else if (!trip) parent.tripThreshold = PApplet.parseFloat(mappedVal) / (boxLen);
        else if (trip) parent.untripThreshold = PApplet.parseFloat(mappedVal) / (boxLen);
      }
    }

    //Checks if mouse is here
    public void overEvent(float tx, float ty) {
      if (overRect(PApplet.parseInt(boxx + tx), PApplet.parseInt(boxy + ty), wid, len)) {
        over = true;
      } else {
        over = false;
      }
    }

    //Checks if mouse is pressed
    public void pressEvent() {
      if (over && mousePressed || locked) {
        press = true;
        locked = true;
      } else {
        press = false;
      }
    }

    //Mouse was released
    public void releaseEvent() {
      locked = false;
    }

    //Color selector and cursor setter
    public void setColor() {
      if (over) {
        current_color = color(127, 134, 143);
        if (!drawHand) {
          cursor(HAND);
          drawHand = true;
        }
      } else {

        if (trip) {
          current_color = color(0, 255, 0); //trip switch bar color
        } else {
          current_color = color(255, 0, 0); //untrip switch bar color
        }

        if (drawHand) {
          cursor(ARROW);
          drawHand = false;
        }
      }
    }

    //Helper function to make setting default threshold values easier.
    //Expects a float as input (0.25 is 25%)
    public void setStretchPercentage(float val) {
      stretch = lock(PApplet.parseInt(boxLen - ((boxLen) * val)), 0, boxLen);
    }

    //Displays the thresholds %%%%%
    public void display(float tx, float ty, float tw, float tl) {
      lx = PApplet.parseInt(tx);
      ly = PApplet.parseInt(ty);
      wid = PApplet.parseInt(tw);
      boxLen = PApplet.parseInt(tl);

      fill(255);
      strokeWeight(1);
      stroke(bgColor);
      setColor();
      fill(current_color);
      rect(boxx, boxy, wid, len);

      // rect(lx, ly, wid, len);
    }

    //Check if the mouse is here
    public boolean overRect(int lx, int ly, int twidth, int theight) {
      if (mouseX >= lx && mouseX <= lx+twidth &&
        mouseY >= ly && mouseY <= ly+theight) {

        return true;
      } else {
        return false;
      }
    }

    //Locks the threshold in place
    public int lock(int val, int minv, int maxv) {
      return  min(max(val, minv), maxv);
    }
  };


  public void sendSerial(){
    println("sending");
  //processTripps();
    byte first = 0;
    byte second = 0;
    if (serialOutMatrix != null ){
      String serialString = "<";
      serialString += "0,"; // send single dummy int
      //serialOutMatrix.write(startMarker);
       //first8 = int(serialChannels[0]);
      for(int b = 0 ; b < 8; b++){
        first += PApplet.parseInt(serialChannels[b]) << b;
        //first8 = 1 << b;
          
      }
       //println("first8 " +binary(first));
      for(int t = 0 ; t < 8; t++){
        
        second += PApplet.parseInt(serialChannels[t+8]) << t;
          
      } 
      println("second8 " + binary(second));
      
      
      
      for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
      serialString += str(serialChannels[i]) ;
          
      }
      serialString += '>';
      try {
        serialOutMatrix.write(first);
        serialOutMatrix.write(second);
        //println(serialString);
        int j = PApplet.parseInt(binary(0000000000000001));
        //serialOutMatrix.write(first);
      }
      catch(RuntimeException e){
         
      }
      
      
      //serialOutMatrix.write(serialChannels[w_matrix.motorWidgets.length]);
      //serialOutMatrix.write(endMarker);
      
    }
  }
  public void playVoice(int channel){
    try {
      voice.pause();
    } catch (Exception e) {
      e.printStackTrace(); 
    }
    
    
     
    switch(channel) {

      case 0:
        voice = soundMinim.loadFile(dataPath("m_yes.mp3"));
        voice.play(); 
        watchBeat = false;
        break; 
      case 1:
        voice = soundMinim.loadFile(dataPath("m_no.mp3"));
        voice.play(); 
        watchBeat = false;
      break; 
      case 12:
        serialChannels[12] = true;
        sendSerial();
        voice = soundMinim.loadFile(dataPath("sure.mp3"));
        voice.play();
        delay(2000);
        voice = soundMinim.loadFile(dataPath("response1.mp3"));
        voice.play();
        beat = new BeatDetect(voice.bufferSize(), voice.sampleRate());
        beat.setSensitivity(100);  
        kickSize = snareSize = hatSize = 16;
        // make a new beat listener, so that we won't miss any buffers for the analysis
        bl = new BeatListener(beat, voice);
        watchBeat = true;
        
      break;
      
      case 13:
        voice = soundMinim.loadFile(dataPath("m_i_am_not_sure.mp3"));
        voice.play();
        watchBeat = false;
      break; 
    }
  
  }
  

  //===================== DIGITAL EVENTS =============================
  //=  Digital Events work by tripping certain thresholds, and then  =
  //=  untripping said thresholds. In order to use digital events    =
  //=  you will need to observe the switchCounter field in any       =
  //=  given channel. Check out the OpenBionics Switch Example       =
  //=  in the process() function above to get an idea of how to do   =
  //=  this. It is important that your observation of switchCounter  =
  //=  is done in the process() function AFTER the Digital Events    =
  //=  are evoked.                                                   =
  //=                                                                =
  //=  This system supports both digital and analog events           =
  //=  simultaneously and seperated.                                 =
  //==================================================================
   public void digitalEventChanHander(int channel,Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
    float timeOfLastOff = cfc.timeOfLastOff;
    //Custom waiting threshold
    int timeToWaitThresh = 1000;
    int timeToWaitOffThresh = 1000;
    int channelselector = channel +1;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      println("Chan " + channelselector +": TRIPPED");
      serialChannels[channelselector-1] = true;
       if ((channelselector == 1) | (channelselector == 2) | (channelselector == 13) | (channelselector == 14) ) processTripps();
       sendSerial(); 
     
      
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold && millis() - timeOfLastOff >= timeToWaitOffThresh) {
      //Untripped
      cfc.switchTripped = false;
      cfc.timeOfLastOff = millis();
      println("Chan " + channelselector +": OFF");
      serialChannels[channelselector-1] = false;
      sendSerial(); 
      
    }
    
    
  } 
   
   
   
   
   
   
 


  //===================== ANALOG EVENTS ===========================
  //=  Analog events are a big more complicated than digital      =
  //=  events. In order to use analog events you must map the     =
  //=  output_normalized value to whatver minimum and maximum     =
  //=  you'd like and then write that to the serialOutMatrix.        =
  //=                                                             =
  //=  Check out analogEventChan0() for the OpenBionics analog    =
  //=  event example to get an idea of how to use analog events.  =
  //===============================================================

  //Channel 1 Event
  public void analogEventChan0(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;


    //================= OpenBionics Analog Movement Example =======================
    //if (serialOutMatrix != null) {
    //  //println("Output normalized: " + int(map(output_normalized, 0, 1, 0, 100)));
    //  if (int(map(output_normalized, 0, 1, 0, 100)) > 10) {
    //    //serialOutMatrix.write("G0P" + int(map(output_normalized, 0, 1, 0, 100)));
    //    delay(10);
    //  } else serialOutMatrix.write("G0P0");
    //}
  }
//
  //Channel 2 Event
  public void analogEventChan1(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 3 Event
  public void analogEventChan2(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 4 Event
  public void analogEventChan3(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 5 Event
  public void analogEventChan4(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 6 Event
  public void analogEventChan5(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 7 Event
  public void analogEventChan6(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 8 Event
  public void analogEventChan7(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 9 Event
  public void analogEventChan8(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 10 Event
  public void analogEventChan9(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 11 Event
  public void analogEventChan10(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 12 Event
  public void analogEventChan11(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 13 Event
  public void analogEventChan12(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 14 Event
  public void analogEventChan13(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 15 Event
  public void analogEventChan14(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 16 Event
  public void analogEventChan15(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }
};


public void MatrixChannelSelection(int n) {
  w_matrix.currChannel = n;
  closeAllDropdowns();
}

public void MatrixEventType(int n) {
  if (n == 0) w_matrix.events[w_matrix.currChannel] = true;
  else if (n == 1) w_matrix.events[w_matrix.currChannel] = false;
  closeAllDropdowns();
}

public void MatrixBaudRate(int n) {
  if (!w_matrix.baudList.get(n).equals("NONE")) w_matrix.theBaud = Integer.parseInt(w_matrix.baudList.get(n));
  closeAllDropdowns();
}

public void MatrixSerialSelection(int n) {
  if (!w_matrix.serList.get(n).equals("NONE")) w_matrix.theSerial = w_matrix.serList.get(n);
  //w_matrix.theSerial = w_matrix.serList.get(n);
  println(w_matrix.theSerial);
  closeAllDropdowns();
}
//////////////////////////////////////////////////////////////////////////
//
//    Networking
//    - responsible for sending data over a Network
//    - Three types of networks are available:
//        - UDP
//        - OSC
//        - LSL
//    - In control panel, specify the network and parameters (port, ip, etc).
//      Then, you can receive the streamed data in a variety of different
//      programs, given that you specify the correct parameters to receive
//      the data stream in those networks.
//
//////////////////////////////////////////////////////////////////////////

float[] data_to_send;
float[] aux_to_send;
float[] full_message;

public void sendRawData_dataPacket(DataPacket_ADS1299 data, float scale_to_uV, float scale_for_aux) {
  data_to_send = writeValues(data.values,scale_to_uV);
  aux_to_send = writeValues(data.auxValues,scale_for_aux);

  full_message = compressArray(data);     //Collect packet into full_message array

  //send to appropriate network type
  if (networkType == 1){
    udp.send_message(data_to_send);       //Send full message to udp
  }else if (networkType == 2){
    osc.send_message(data_to_send);       //Send full message to osc
  }else if (networkType == 3){
    lsl.send_message(data_to_send,aux_to_send);       //Send
  }
}
// Convert counts to scientific values (uV or G)
private float[] writeValues(int[] values, float scale_fac) {
  int nVal = values.length;
  float[] temp_buffer = new float[nVal];
  for (int Ival = 0; Ival < nVal; Ival++) {
    temp_buffer[Ival] = scale_fac * PApplet.parseFloat(values[Ival]);
  }
  return temp_buffer;
}

//Package all data into one array (full_message) for UDP and OSC
private float[] compressArray(DataPacket_ADS1299 data){
    full_message = new float[1 + data_to_send.length + aux_to_send.length];
    full_message[0] = data.sampleIndex;
    for (int i=0;i<data_to_send.length;i++){
      full_message[i+1] = data_to_send[i];
    }
    for (int i=0;i<aux_to_send.length;i++){
      full_message[data_to_send.length + 1] = aux_to_send[i];
    }
    return full_message;
}

//////////////
// CLASSES //

/**
 * To perform any action on datagram reception, you need to implement this
 * handler in your code. This method will be automatically called by the UDP
 * object each time he receive a nonnull message. This method will send the
 * message to `udpEvent`
 */
// void receive(byte[] data, String ip, int port) {	// <-- extended handler
//   // get the "real" message =
//   // forget the ";\n" at the end <-- !!! only for a communication with Pd !!!
//   data = subset(data, 0, data.length-2);
//   String message = new String( data );
//
//   // Be safe, always check to make sure the parent did implement this function
//   if (ganglion.udpRx.udpEventMethod != null) {
//     try {
//       ganglion.udpRx.udpEventMethod.invoke(ganglion.udpRx.parent, message);
//     }
//     catch (Exception e) {
//       System.err.println("Disabling udpEvent() for because of an error.");
//       e.printStackTrace();
//       ganglion.udpRx.udpEventMethod = null;
//     }
//   }
// }

// void clientEvent(Client someClient) {
//   print("Server Says:  ");
//   dataIn = myClient.read();
//   println(dataIn);
//   background(dataIn);
//
//   // get the "real" message =
//   // forget the ";\n" at the end <-- !!! only for a communication with Pd !!!
//   data = subset(data, 0, data.length-2);
//   String message = new String( data );
//
//   // Be safe, always check to make sure the parent did implement this function
//   if (ganglion.udpRx.udpEventMethod != null) {
//     try {
//       ganglion.udpRx.udpEventMethod.invoke(ganglion.udpRx.parent, message);
//     }
//     catch (Exception e) {
//       System.err.println("Disabling udpEvent() for because of an error.");
//       e.printStackTrace();
//       ganglion.udpRx.udpEventMethod = null;
//     }
//   }
//
// }

class UDPReceive {
  public Method udpEventMethod;
  public PApplet parent;
  int port;
  String ip;
  boolean listen;
  UDP udp;

  /**
   * @description Used to construct a new UDP connection
   * @param `parent` {PApplet} - The object calling constructor. Implements
   *  `udpEvent` if `parent` wants to recieve messages.
   * @param `port` {int} - The port number to use for the UDP port
   * @param `ip` {String} - The ip address for the UDP connection. Use `localhost`
   *  to keep the port on this computer.
   * @constructor
   */
  public UDPReceive(PApplet parent, int port, String ip) {
    // Grab vars
    this.port  = port;
    this.ip = ip;

    this.udp = new UDP(parent, port);
    println("udp bound to " + port);
    this.udp.setBuffer(1024);
    this.udp.log(false);
    this.udp.listen(true);

    // callback: https://forum.processing.org/one/topic/noob-q-i-d-like-to-learn-more-about-callbacks.html
    // Set parent for callback
    this.parent = parent;

    // Verify that parent actaully implements the callback
    try {
      this.udpEventMethod = this.parent.getClass().getMethod("udpEvent", new Class[] { String.class });
      println("Networking: Good job iplmenting udpEvent callback in parent " + parent);
    }
    catch (Exception e) {
      // No such method declared, there for the parent who created this will not
      //  recieve messages :(
      println("Networking: Error failed to implement udpEvent callback in parent " + this.parent);
      this.udp.listen(false);
    }

  }
}

// UDP SEND //
class UDPSend {
  int port;
  String ip;
  UDP udp;

  UDPSend(int _port, String _ip){
    port = _port;
    ip = _ip;
    udp = new UDP(this);
    udp.setBuffer(1024);
    udp.log(false);
  }
  public void send_message(float[] _message){
    String message = Arrays.toString(_message);
    udp.send(message,ip,port);
  }

  public void send(String msg){
    udp.send(msg,ip,port);
  }
}

// OSC SEND //
class OSCSend{
  int port;
  String ip;
  String address;
  OscP5 osc;
  NetAddress netaddress;

  OSCSend(int _port, String _ip, String _address){
    port = _port;
    ip = _ip;
    address = _address;
    osc = new OscP5(this,12000);
    netaddress = new NetAddress(ip,port);
  }
  public void send_message(float[] _message){
    OscMessage osc_message = new OscMessage(address);
    osc_message.add(_message);
    osc.send(osc_message, netaddress);
  }
}

// LSL SEND //
class LSLSend{
  String data_stream;
  String data_stream_id;
  String aux_stream;
  String aux_stream_id;
  LSL.StreamInfo info_data;
  LSL.StreamOutlet outlet_data;
  LSL.StreamInfo info_aux;
  LSL.StreamOutlet outlet_aux;

  LSLSend(String _data_stream, String _aux_stream){
    data_stream = _data_stream;
    data_stream_id = data_stream + "_id";
    aux_stream = _aux_stream;
    aux_stream_id = aux_stream + "_id";
    info_data = new LSL.StreamInfo(data_stream, "EEG", nchan, openBCI.get_fs_Hz(), LSL.ChannelFormat.float32, data_stream_id);
    outlet_data = new LSL.StreamOutlet(info_data);
    //info_aux = new LSL.StreamInfo("aux_stream", "AUX", 3, openBCI.get_fs_Hz(), LSL.ChannelFormat.float32, aux_stream_id);
    //outlet_aux = new LSL.StreamOutlet(info_aux);
  }
  public void send_message(float[] _data_message, float[] _aux_message){
    outlet_data.push_sample(_data_message);
    //outlet_aux.push_sample(_aux_message);
  }
}
//////////////////////////////////////////////////////////////////////////
//
//		Playground Class
//		Created: 11/22/14 by Conor Russomanno
//		An extra interface pane for additional GUI features
//
//////////////////////////////////////////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

Playground playground;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------

class Playground {

  //button for opening and closing
  float x, y, w, h;
  int boxBG;
  int strokeColor;
  float topMargin, bottomMargin;

  boolean isOpen;
  boolean collapsing;

  Button collapser;

  Playground(int _topMargin) {

    topMargin = _topMargin;
    bottomMargin = helpWidget.h;

    isOpen = false;
    collapsing = true;

    boxBG = color(255);
    strokeColor = color(138, 146, 153);
    collapser = new Button(0, 0, 20, 60, "<", 14);

    x = width;
    y = topMargin;
    w = 0;
    h = height - (topMargin+bottomMargin);
  }

  public void update() {
    // verbosePrint("uh huh");
    if (collapsing) {
      collapse();
    } else {
      expand();
    }
    
    // if(accelWidget.collapsing) accelWidget.collapse();
    // else accelWidget.expand();
    
    //if(pulseWidget.collapsing) pulseWidget.collapse();
    //else pulseWidget.expand();

    if (x > width) {
      x = width;
    }
  }

  public void draw() {
    // verbosePrint("yeaaa");
    pushStyle();
    fill(boxBG);
    stroke(strokeColor);
    rect(width - w, topMargin, w, height - (topMargin + bottomMargin));
    textFont(f1);
    textAlign(LEFT, TOP);
    fill(bgColor);
    text("Developer Playground", x + 10, y + 10);
    fill(255, 0, 0);
    //uncomment if you want the dev playground to display again
    //collapser.draw(int(x - collapser.but_dx), int(topMargin + (h-collapser.but_dy)/2));
    popStyle();
  }

  public boolean isMouseHere() {
    if (mouseX >= x && mouseX <= width && mouseY >= y && mouseY <= height - bottomMargin) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isMouseInButton() {
    //verbosePrint("Playground: isMouseInButton: attempting");
    if (mouseX >= collapser.but_x && mouseX <= collapser.but_x+collapser.but_dx && mouseY >= collapser.but_y && mouseY <= collapser.but_y + collapser.but_dy) {
      return true;
    } else {
      return false;
    }
  }

  public void toggleWindow() {
    
    //Uncomment if you'd like to open the playground
    //if (isOpen) {//if open
    //  verbosePrint("close");
    //  collapsing = true;//collapsing = true;
    //  isOpen = false;
    //  collapser.but_txt = "<";
    //} else {//if closed
    //  verbosePrint("open");
    //  collapsing = false;//expanding = true;
    //  isOpen = true;
    //  collapser.but_txt = ">";
    //}
    
    //if(drawAccel){
    //  if (accelWidget.isOpen) {//if open
    //    verbosePrint("close");
    //    accelWidget.collapsing = true;//collapsing = true;
    //    accelWidget.isOpen = false;
    //    accelWidget.collapser.but_txt = "<";
    //  } else {//if closed
    //    verbosePrint("open");
    //    accelWidget.collapsing = false;//expanding = true;
    //    accelWidget.isOpen = true;
    //    accelWidget.collapser.but_txt = ">";
    //  }
    //}
    
    //if(drawPulse){
    //  if (pulseWidget.isOpen) {//if open
    //    verbosePrint("close");
    //    pulseWidget.collapsing = true;//collapsing = true;
    //    pulseWidget.isOpen = false;
    //    pulseWidget.collapser.but_txt = "<";
    //  } else {//if closed
    //    verbosePrint("open");
    //    pulseWidget.collapsing = false;//expanding = true;
    //    pulseWidget.isOpen = true;
    //    pulseWidget.collapser.but_txt = ">";
    //  }
    //}
    
  }

  public void mousePressed() {
    //verbosePrint("Playground >> mousePressed()");
  }

  public void mouseReleased() {
    //verbosePrint("Playground >> mouseReleased()");
  }

  public void expand() {
    if (w <= width/3) {
      w = w + 50;
      x = width - w;
    }
  }

  public void collapse() {
    if (w >= 0) {
      w = w - 50;
      x = width - w;
    }
  }
};


///////////////////////////////////////////////////////////////////////////
//
//     Created: 2/19/16
//     by Conor Russomanno for BodyHacking Con DIY Cyborgia Presentation
//     This code is used to organize a neuro-powered presentation... refer to triggers in the EEG_Processing_User class of the EEG_Processing.pde file
//
///////////////////////////////////////////////////////////////////////////

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------

Presentation myPresentation;
boolean drawPresentation = false;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------

class Presentation {
  //presentation images
  int slideCount = 4;
  //PImage presentationSlides[] = new PImage[slideCount];
  float timeOfLastSlideChange = 0;
  int currentSlide = 0;
  boolean lockSlides = false;

  Presentation (){
    //loading presentation images
    println("attempting to load images for presentation...");
    //presentationSlides[0] = loadImage("prez-images/Presentation.000.jpg");
    //presentationSlides[1] = loadImage("prez-images/Presentation.001.jpg");
    //presentationSlides[2] = loadImage("prez-images/Presentation.002.jpg");
    //presentationSlides[3] = loadImage("prez-images/Presentation.003.jpg");
    
    
    // presentationSlides[4] = loadImage("prez-images/Presentation.004.jpg");
    // presentationSlides[5] = loadImage("prez-images/Presentation.005.jpg");
    // presentationSlides[6] = loadImage("prez-images/Presentation.006.jpg");
    // presentationSlides[7] = loadImage("prez-images/Presentation.007.jpg");
    // presentationSlides[8] = loadImage("prez-images/Presentation.008.jpg");
    // presentationSlides[9] = loadImage("prez-images/Presentation.009.jpg");
    // presentationSlides[10] = loadImage("prez-images/Presentation.010.jpg");
    // presentationSlides[11] = loadImage("prez-images/Presentation.011.jpg");
    // presentationSlides[12] = loadImage("prez-images/Presentation.012.jpg");
    // presentationSlides[13] = loadImage("prez-images/Presentation.013.jpg");
    // presentationSlides[14] = loadImage("prez-images/Presentation.014.jpg");
    // presentationSlides[15] = loadImage("prez-images/Presentation.015.jpg");
    // slideCount = 4;
    println("DONE loading images!");
  }

  public void slideForward() {
    if(currentSlide < slideCount - 1 && drawPresentation && !lockSlides){
      println("Slide Forward!");
      currentSlide++;
    } else{
      println("No more slides. Can't go forward...");
    }
  }

  public void slideBack() {
    if(currentSlide > 0 && drawPresentation && !lockSlides){
      println("Slide Back!");
      currentSlide--;
    } else {
      println("On the first slide. Can't go back...");
    }
  }

  public void draw() {
      // ----- Drawing Presentation -------
    pushStyle();

    //image(presentationSlides[currentSlide], 0, 0, width, height);


    if(lockSlides){
      //draw red rectangle to indicate that slides are locked
      pushStyle();
      fill(255,0,0);
      rect(width - 50, 25, 25, 25);
      popStyle();
    }

    textFont(p3, 16);
    fill(openbciBlue);
    textAlign(CENTER);
    text("Press [Enter] to exit presentation mode.", width/2, 31*(height/32));

    popStyle();
  }
}
/////////////////////////////////////////////////////////////////////////////////
//
//  PulseSensor_Widget is used to visiualze heartbeat data using a pulse sensor
//
//  Created: Colin Fausnaught, September 2016
//           Source Code by Joel Murphy
//
//  Use '/' to toggle between accelerometer and pulse sensor.
////////////////////////////////////////////////////////////////////////////////

class PulseSensor_Widget{

  //button for opening and closing
  int x, y, w, h;
  int parentContainer = 3;
  int boxBG;
  int strokeColor;

// Pulse Sensor Stuff
  int count = 0;
  int heart = 0;
  int PulseBuffSize = 500;

  int PulseWindowWidth;
  int PulseWindowHeight;
  int PulseWindowX;
  int PulseWindowY;
  int eggshell;
  int[] PulseWaveY;      // HOLDS HEARTBEAT WAVEFORM DATA
  boolean rising;
  //boolean OBCI_inited= false;

  //OpenBCI_ADS1299 OBCI;

  PulseSensor_Widget(PApplet parent) {
    x = (int)container[parentContainer].x;
    y = (int)container[parentContainer].y;
    w = (int)container[parentContainer].w;
    h = (int)container[parentContainer].h;

    boxBG = bgColor;
    strokeColor = color(138, 146, 153);

    // Pulse Sensor Stuff
    eggshell = color(255, 253, 248);

    PulseWindowWidth = 500;
    PulseWindowHeight = 183;
    PulseWindowX = PApplet.parseInt(x)+5;
    PulseWindowY = PApplet.parseInt(y)-10+PApplet.parseInt(h)/2;
    PulseWaveY = new int[PulseBuffSize];
    rising = true;
    for (int i=0; i<PulseWaveY.length; i++){
      PulseWaveY[i] = PulseWindowY + PulseWindowHeight/2; // initialize the pulse window data line to V/2
   }

  }

  public void update() {
    if (isRunning) {
      if(synthesizeData){
        count++;
      }

      if(frameCount%60 == 0){ heart = 15; }  // fake the beat for now
      if(openBCI.freshAuxValues){ heart = 4; }
      heart--;                    // heart is used to time how long the heart graphic swells when your heart beats
      heart = max(heart,0);       // don't let the heart variable go into negative numbers

      float upperClip = 800.0f;  // used to keep the pulse waveform within the pulse wave window
      float lowerClip = 200.0f;
      if(synthesizeData){
        if(rising){  // MAKE A SAW WAVE FOR TESTING
         PulseWaveY[PulseWaveY.length-1]--;   // place the new raw datapoint at the end of the array
         if(PulseWaveY[PulseWaveY.length-1] == PulseWindowY){ rising = false; }
        }else{
         PulseWaveY[PulseWaveY.length-1]++;   // place the new raw datapoint at the end of the array
         if(PulseWaveY[PulseWaveY.length-1] == PulseWindowY+PulseWindowHeight){ rising = true; }
        }
      }else{
        float sensorValue = PApplet.parseFloat(openBCI.rawReceivedDataPacket.auxValues[0]);
        PulseWaveY[PulseWaveY.length-1] =
        PApplet.parseInt(map(sensorValue,lowerClip,upperClip,PApplet.parseFloat(PulseWindowY+PulseWindowHeight),PApplet.parseFloat(PulseWindowY)));
        PulseWaveY[PulseWaveY.length-1] = constrain(PulseWaveY[PulseWaveY.length-1],PulseWindowY,PulseWindowY+PulseWindowHeight);
      }

      for (int i = 0; i < PulseWaveY.length-1; i++) {      // move the pulse waveform by
       PulseWaveY[i] = PulseWaveY[i+1];
      }
    }
  }

  public void draw() {
    if(drawPulse){
    // verbosePrint("yeaaa");
      fill(boxBG);
      stroke(strokeColor);
      rect(x, y, w, h);

      textFont(f4,24);
      textAlign(LEFT, TOP);
      fill(eggshell);
      text("Pulse Sensor Amped", x + 10, y + 10);
      textFont(f4,32);
      if(synthesizeData){
        text("BPM " + count, x+10, y+50);
        text("IBI 760", x+10, y+100);
        //text("Width "+ w, x+10, y+50);
        //text("Height "+ h, x+10, y+70);
      }else{
        text("BPM " + openBCI.validAuxValues[1], x+10, y+40);
        text("IBI " + openBCI.validAuxValues[2], x+10, y+100);
      }

      // heart shape
      fill(250,0,0);
      stroke(250,0,0);

      strokeWeight(1);
      if (heart > 0){             // if a beat happened recently,
      strokeWeight(8);          // make the heart pulse
      }

      translate(-35,0);
      smooth();   // draw the heart with two bezier curves
      bezier(x+w-60,y+40, x+w+20,y-30, x+w+40,y+130, x+w-60,y+140);
      bezier(x+w-60,y+40, x+w-150,y-30, x+w-160,y+130, x+w-60,y+140);
      translate(35,0);

      strokeWeight(1);          // reset the strokeWeight for next time
      fill(eggshell);  // pulse window background
      stroke(eggshell);
      rect(PulseWindowX,PulseWindowY,PulseWindowWidth,PulseWindowHeight);

      stroke(255,0,0);                               // red is a good color for the pulse waveform
      noFill();
      beginShape();                                  // using beginShape() renders fast
      for (int x = 0; x < PulseWaveY.length; x++) {
        int xi = PApplet.parseInt(map(x, 0, PulseWaveY.length-1, 0, PulseWindowWidth-1));
        vertex(PulseWindowX+xi, PulseWaveY[x]);                    //draw a line connecting the data points
      }
      endShape();

    }
  }

  public void screenResized(PApplet _parent, int _winX, int _winY) {
    //when screen is resized...
    //update position/size of Pulse Widget
    x = (int)container[parentContainer].x;
    y = (int)container[parentContainer].y;
    w = (int)container[parentContainer].w;
    h = (int)container[parentContainer].h;


    PulseWindowX = PApplet.parseInt(x)+5;
    PulseWindowY = PApplet.parseInt(y)-10+PApplet.parseInt(h)/2;
    PulseWindowWidth = PApplet.parseInt(w)-10;
    PulseWindowHeight = 183;
  }

  //boolean isMouseHere() {
  //  if (mouseX >= x && mouseX <= width && mouseY >= y && mouseY <= height - bottomMargin) {
  //    return true;
  //  } else {
  //    return false;
  //  }
  //}

  //boolean isMouseInButton() {
  //  //verbosePrint("Playground: isMouseInButton: attempting");
  //  if (mouseX >= collapser.but_x && mouseX <= collapser.but_x+collapser.but_dx && mouseY >= collapser.but_y && mouseY <= collapser.but_y + collapser.but_dy) {
  //    return true;
  //  } else {
  //    return false;
  //  }
  //}

  public void mousePressed() {
    verbosePrint("PulseSensor >> mousePressed()");
  }

  public void mouseReleased() {
    verbosePrint("PulseSensor >> mouseReleased()");
  }

}
/////////////////////////////////////////////////////////////////////////////////
//
//  Radios_Config will be used for radio configuration
//  integration. Also handles functions such as the "autconnect"
//  feature.
//
//  Created: Colin Fausnaught, July 2016
//
//  Handles interactions between the radio system and OpenBCI systems.
//  It is important to note that this is using Serial communication directly
//  rather than the OpenBCI_ADS1299 class. I just found this easier to work
//  with.
//
//  Modified by Joel Murphy, January 2017
//
//
//  KNOWN ISSUES:
//
//  TODO:
////////////////////////////////////////////////////////////////////////////////
boolean isOpenBCI;
int baudSwitch = 0;

public void autoconnect(){
    //Serial locBoard; //local serial instance just to make sure it's openbci, then connect to it if it is
    String[] serialPorts = new String[Serial.list().length];
    String serialPort  = "";
    serialPorts = Serial.list();


    for(int i = 0; i < serialPorts.length; i++){
    // for(int i = serialPorts.length-1; i >= 0; i--){
      try{
          serialPort = serialPorts[i];
          board = new Serial(this,serialPort,115200);
          print("try "); print(i); print(" "); print(serialPort); println(" at 115200 baud");
          output("Attempting to connect at 115200 baud to " + serialPort);  // not working
          delay(5000);

          // board.write('?');
          board.write('v'); //modified by JAM 1/17
          //board.write(0x07);
          delay(2000);
          if(confirm_openbci()) {
            println("Board connected on port " +serialPorts[i] + " with BAUD 115200");
            output("Connected to " + serialPort + "!");
            openBCI_portName = serialPorts[i];
            openBCI_baud = 115200;
            board.stop();
            return;
          } else {
            println("Board not on port " + serialPorts[i] +" with BAUD 115200");
            board.stop();
          }
        }
        catch (Exception e){
          println("Exception " + serialPorts[i] + " " + e);
        }

      try{
          board = new Serial(this,serialPort,230400);
          print("try "); print(i); print(" "); print(serialPort); println(" at 230400 baud");
          output("Attempting to connect at 230400 baud to " + serialPort);  // not working
          delay(5000);

          // board.write('?');
          board.write('v'); //modified by JAM 1/17
          //board.write(0x07);
          delay(2000);
          if(confirm_openbci()) {  // was just confrim_openbci  JAM 1/2017
            println("Board connected on port " +serialPorts[i] + " with BAUD 230400");
            output("Connected to " + serialPort + "!"); // not working
            openBCI_baud = 230400;
            openBCI_portName = serialPorts[i];
            board.stop();
            return;
          } else {
            println("Board not on port " + serialPorts[i] +" with BAUD 230400");
            board.stop();
          }

        }
        catch (Exception e){
          println("Exception " + serialPorts[i] + " " + e);
        }
    }
}

// Serial autoconnect_return_default() throws Exception{
//
//     Serial locBoard; //local serial instance just to make sure it's openbci, then connect to it if it is
//     Serial retBoard;
//     String[] serialPorts = new String[Serial.list().length];
//     String serialPort  = "";
//     serialPorts = Serial.list();
//
//
//     for(int i = 0; i < serialPorts.length; i++){
//
//       try{
//           serialPort = serialPorts[i];
//           locBoard = new Serial(this,serialPort,115200);
//
//           delay(100);
//
//           locBoard.write(0xF0);
//           locBoard.write(0x07);
//           delay(1000);
//
//           if(confirm_openbci_v2()) {
//             println("Board connected on port " +serialPorts[i] + " with BAUD 115200");
//             no_start_connection = true;
//             openBCI_portName = serialPorts[i];
//             openBCI_baud = 115200;
//             isOpenBCI = false;
//
//             return locBoard;
//           }
//           else locBoard.stop();
//         }
//         catch (Exception e){
//           println("Board not on port " + serialPorts[i] +" with BAUD 115200");
//         }
//     }
//
//
//     throw new Exception();
// }

// Serial autoconnect_return_high() throws Exception{
//
//     Serial localBoard; //local serial instance just to make sure it's openbci, then connect to it if it is
//     String[] serialPorts = new String[Serial.list().length];
//     String serialPort  = "";
//     serialPorts = Serial.list();
//
//
//     for(int i = 0; i < serialPorts.length; i++){
//       try{
//           serialPort = serialPorts[i];
//           localBoard = new Serial(this,serialPort,230400);
//
//           delay(100);
//
//           localBoard.write(0xF0);
//           localBoard.write(0x07);
//           delay(1000);
//           if(confirm_openbci_v2()) {
//             println("Board connected on port " +serialPorts[i] + " with BAUD 230400");
//             no_start_connection = true;
//             openBCI_portName = serialPorts[i];
//             openBCI_baud = 230400;
//             isOpenBCI = false;
//
//             return localBoard;
//           }
//         }
//         catch (Exception e){
//           println("Board not on port " + serialPorts[i] +" with BAUD 230400");
//         }
//
//     }
//     throw new Exception();
// }

/**** Helper function for connection of boards ****/
public boolean confirm_openbci(){
  //println(board_message.toString());
  // if(board_message.toString().toLowerCase().contains("registers")) return true;
  // print("board "); print(board_message.toString()); println("message");
  if(board_message != null){
    if(board_message.toString().toLowerCase().contains("ads")){
      return true;
    }
  }
  return false;
}

public boolean confirm_openbci_v2(){
  //println(board_message.toString());
  if(board_message.toString().toLowerCase().contains("success"))  return true;
  // if(board_message.toString().contains("v2."))  return true;
  else return false;
}
/**** Helper function for autoscan ****/
public boolean confirm_connected(){
  if( board_message != null && board_message.toString().toLowerCase().contains("success")) return true; // JAM added .containes("success")
  else return false;
}

/**** Helper function to read from the serial easily ****/
public boolean print_bytes(RadioConfigBox rc){
  if(board_message != null){
    println(board_message.toString());
    rc.print_onscreen(board_message.toString());
    return true;
  } else {
    return false;
  }
}

public void print_bytes_error(RadioConfigBox rcConfig){
  println("Error reading from Serial/COM port");
  rcConfig.print_onscreen("Error reading from Serial port. Try a different port?");
  board = null;
}

/**** Function to connect to a selected port ****/  // JAM 1/2017
//    Needs to be connected to something to perform the Radio_Config tasks
public boolean connect_to_portName(RadioConfigBox rcConfig){
  if(openBCI_portName != "N/A"){
    output("Attempting to open Serial/COM port: " + openBCI_portName);
    try {
      println("Radios_Config: connect_to_portName: attempting to open serial port: " + openBCI_portName);
      serial_output = new Serial(this,openBCI_portName,openBCI_baud); //open the com port
      serial_output.clear(); // clear anything in the com port's buffer
      // portIsOpen = true;
      println("Radios_Config: connect_to_portName: port is open!");
      // changeState(STATE_COMINIT);
      board = serial_output;
      return true;
    }
    catch (RuntimeException e){
      if (e.getMessage().contains("<init>")) {
        serial_output = null;
        System.out.println("Radios_Config: connect_to_portName: port in use, trying again later...");
        // portIsOpen = false;
      } else{
        println("RunttimeException: " + e);
        output("Error connecting to selected Serial/COM port. Make sure your board is powered up and your dongle is plugged in.");
        rcConfig.print_onscreen("Error connecting to Serial port. Try a different port?");
      }
      board = null;
      println("Radios_Config: connect_to_portName: failed to connect to " + openBCI_portName);
      return false;
    }
  } else {
    output("No Serial/COM port selected. Please select your Serial/COM port and retry");
    rcConfig.print_onscreen("Select a Serial/COM port, then try again");
    return false;
  }
}



//=========== GET SYSTEM STATUS ============
//= Get's the current status of the system
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x07).
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

public void system_status(RadioConfigBox rcConfig){
  if(board == null){
    if(!connect_to_portName(rcConfig)){
      return;
    }
  }
  if(board != null){
    board.write(0xF0);
    board.write(0x07);
    delay(100);
    if(!print_bytes(rcConfig)){
      print_bytes_error(rcConfig);
    }
  } else {
    println("Error, no board connected");
    rcConfig.print_onscreen("No board connected!");
  }
}

//Scans through channels until a success message has been found
public void scan_channels(RadioConfigBox rcConfig){
  if(board == null){
    if(!connect_to_portName(rcConfig)){
      return;
    }
  }
  for(int i = 1; i < 26; i++){

    set_channel_over(rcConfig,i);
    system_status(rcConfig);
    if(confirm_connected()) return; // break;
  }
}



//============== GET CHANNEL ===============
//= Gets channel information from the radio.
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x00).
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

public void get_channel(RadioConfigBox rcConfig){
  if(board == null){
    if(!connect_to_portName(rcConfig)){
      return;
    }
  }

  if(board != null){
    board.write(0xF0);
    board.write(0x00);
    delay(100);
    if(!print_bytes(rcConfig)){
      print_bytes_error(rcConfig);
    }
  }
  else {
    println("Error, no board connected");
    rcConfig.print_onscreen("No board connected!");
  }
  }

//============== SET CHANNEL ===============
//= Sets the radio and board channel.
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x01) followed by the number to
//= set the board and radio to. Channels can
//= only be 1-25.
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

public void set_channel(RadioConfigBox rcConfig, int channel_number){
  if(board == null){
    if(!connect_to_portName(rcConfig)){
      return;
    }
  }
  if(board != null){
    if(channel_number > 0){
      board.write(0xF0);
      board.write(0x01);
      board.write(PApplet.parseByte(channel_number));
      delay(1000);
      if(!print_bytes(rcConfig)){
        print_bytes_error(rcConfig);
      }
    }
    else rcConfig.print_onscreen("Please Select a Channel");
  }
  else {
    println("Error, no board connected");
    rcConfig.print_onscreen("No board connected!");
  }
}

//========== SET CHANNEL OVERRIDE ===========
//= Sets the radio channel only
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x02) followed by the number to
//= set the board and radio to. Channels can
//= only be 1-25.
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

public void set_channel_over(RadioConfigBox rcConfig, int channel_number){
  if(board == null){
    if(!connect_to_portName(rcConfig)){
      return;
    }
  }
  if(board != null){
    if(channel_number > 0){
      board.write(0xF0);
      board.write(0x02);
      board.write(PApplet.parseByte(channel_number));
      delay(100);
      if(!print_bytes(rcConfig)){
        print_bytes_error(rcConfig);
      }
    }

    else rcConfig.print_onscreen("Please Select a Channel");
  }

  else {
    println("Error, no board connected");
    rcConfig.print_onscreen("No board connected!");
  }
}

//================ GET POLL =================
//= Gets the poll time
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x03).
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

// void get_poll(RadioConfigBox rcConfig){
//   if(board == null){
//     if(!connect_to_portName(rcConfig)){
//       return;
//     }
//   }
//   if(board != null){
//       board.write(0xF0);
//       board.write(0x03);
//       isGettingPoll = true;
//       delay(100);
//       board_message.append(hexToInt);
//       if(!print_bytes(rcConfig)){
        //   print_bytes_error(rcConfig);
        // }
//       isGettingPoll = false;
//       spaceFound = false;
//   }
//
//   else {
//     println("Error, no board connected");
//     rcConfig.print_onscreen("No board connected!");
//   }
// }

//=========== SET POLL OVERRIDE ============
//= Sets the poll time
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x04) followed by the number to
//= set as the poll value. Channels can only
//= be 0-255.
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

// void set_poll(RadioConfigBox rcConfig, int poll_number){
//   if(board == null){
//     if(!connect_to_portName(rcConfig)){
//       return;
//     }
//   }
//   if(board != null){
//     board.write(0xF0);
//     board.write(0x04);
//     board.write(byte(poll_number));
//     delay(1000);
//     if(!print_bytes(rcConfig)){
  // print_bytes_error(rcConfig);
// }
//   }
//   else {
//     println("Error, no board connected");
//     rcConfig.print_onscreen("No board connected!");
//   }
// }

//========== SET BAUD TO DEFAULT ===========
//= Sets BAUD to it's default value (115200)
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x05).
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

// void set_baud_default(RadioConfigBox rcConfig, String serialPort){
//   if(board == null){
//     if(!connect_to_portName(rcConfig)){
//       return;
//     }
//   }
//   if(board != null){
//     board.write(0xF0);
//     board.write(0x05);
//     delay(1000);
//     if(!print_bytes(rcConfig)){
  // print_bytes_error(rcConfig);
// }
//     delay(1000);
//
//
//     try{
//       board.stop();
//       board = null;
//       board = autoconnect_return_default();
//     }
//     catch (Exception e){
//       println("error setting serial to BAUD 115200");
//     }
//   }
//   else {
//     println("Error, no board connected");
//     rcConfig.print_onscreen("No board connected!");
//   }
// }

//====== SET BAUD TO HIGH-SPEED MODE =======
//= Sets BAUD to a higher rate (230400)
//=
//= First writes 0xF0 to let the board know
//= a command is coming, then writes the
//= command (0x06).
//=
//= After a short delay it then prints bytes
//= from the board.
//==========================================

// void set_baud_high(RadioConfigBox rcConfig, String serialPort){
//   if(board == null){
//     if(!connect_to_portName(rcConfig)){
//       return;
//     }
//   }
//   if(board != null){
//     board.write(0xF0);
//     board.write(0x06);
//     delay(1000);
//    if(!print_bytes(rcConfig)){
  // print_bytes_error(rcConfig);
// }
//     delay(1000);
//
//     try{
//       board.stop();
//       board = null;
//       board = autoconnect_return_high();
//     }
//     catch (Exception e){
//       println("error setting serial to BAUD 230400");
//     }
//   }
//   else {
//     println("Error, no board connected");
//     rcConfig.print_onscreen("No board connected!");
//   }
//
// }
///////////////////////////////////////////////////////////////////////////////////////
//
//  Created by Conor Russomanno, 11/3/16
//  Extracting old code Gui_Manager.pde, adding new features for GUI v2 launch
//
///////////////////////////////////////////////////////////////////////////////////////




int navBarHeight = 32;
TopNav topNav;

class TopNav {

  // PlotFontInfo fontInfo;

  Button controlPanelCollapser;

  Button stopButton;
  public final static String stopButton_pressToStop_txt = "Stop Data Stream";
  public final static String stopButton_pressToStart_txt = "Start Data Stream";

  Button filtBPButton;
  Button filtNotchButton;

  Button tutorialsButton;
  Button shopButton;
  Button issuesButton;


  Button layoutButton;

  LayoutSelector layoutSelector;
  TutorialSelector tutorialSelector;

  boolean finishedInit = false;

  //constructor
  TopNav(){

    controlPanelCollapser = new Button(3, 3, 256, 26, "System Control Panel", fontInfo.buttonLabel_size);
    controlPanelCollapser.setFont(h3, 16);
    controlPanelCollapser.setIsActive(true);
    controlPanelCollapser.isDropdownButton = true;

    //top right buttons from right to left
    //int butNum = 1;
    //tutorialsButton = new Button(width - 3*(butNum) - 80, 3, 80, 26, "Help", fontInfo.buttonLabel_size);
    //tutorialsButton.setFont(h3, 16);
    //tutorialsButton.setHelpText("Here you will find links to helpful online tutorials and getting started guides. Also, check out how to create custom widgets for the GUI!");

    //butNum = 2;
    //issuesButton = new Button(width - 3*(butNum) - 80 - tutorialsButton.but_dx, 3, 80, 26, "Issues", fontInfo.buttonLabel_size);
    //issuesButton.setHelpText("If you have suggestions or want to share a bug you've found, please create an issue on the GUI's Github repo!");
    //issuesButton.setURL("https://github.com/OpenBCI/DAC_GUI_v2.0/issues");
    //issuesButton.setFont(h3, 16);

    //butNum = 3;
    //shopButton = new Button(width - 3*(butNum) - 80 - issuesButton.but_dx - tutorialsButton.but_dx, 3, 80, 26, "Shop", fontInfo.buttonLabel_size);
    //shopButton.setHelpText("Head to our online store to purchase the latest OpenBCI hardware and accessories.");
    //shopButton.setURL("http://shop.openbci.com/");
    //shopButton.setFont(h3, 16);



    layoutSelector = new LayoutSelector();
    tutorialSelector = new TutorialSelector();

    updateNavButtonsBasedOnColorScheme();

  }

  public void initSecondaryNav(){
    stopButton = new Button(3, 35, 170, 26, stopButton_pressToStart_txt, fontInfo.buttonLabel_size);
    stopButton.setFont(h4, 14);
    stopButton.setColorNotPressed(color(184, 220, 105));
    stopButton.setHelpText("Press this button to Stop/Start the data stream. Or press <SPACEBAR>");

    filtNotchButton = new Button(7 + stopButton.but_dx, 35, 70, 26, "Notch\n" + dataProcessing.getShortNotchDescription(), fontInfo.buttonLabel_size);
    filtNotchButton.setFont(p5, 12);
    filtBPButton = new Button(11 + stopButton.but_dx + 70, 35, 70, 26, "BP Filt\n" + dataProcessing.getShortFilterDescription(), fontInfo.buttonLabel_size);
    filtBPButton.setFont(p5, 12);

    //right to left in top right (secondary nav)
    layoutButton = new Button(width - 3 - 60, 35, 60, 26, "Layout", fontInfo.buttonLabel_size);
    layoutButton.setHelpText("Here you can alter the overall layout of the GUI, allowing for different container configurations with more or less widgets.");
    layoutButton.setFont(h4, 14);

    updateSecondaryNavButtonsColor();
  }

  public void updateNavButtonsBasedOnColorScheme(){
    if(colorScheme == COLOR_SCHEME_DEFAULT){
      controlPanelCollapser.setColorNotPressed(color(255));
      //issuesButton.setColorNotPressed(color(255));
      //shopButton.setColorNotPressed(color(255));
      //tutorialsButton.setColorNotPressed(color(255));

      controlPanelCollapser.textColorNotActive = color(bgColor);
      //issuesButton.textColorNotActive = color(bgColor);
      //shopButton.textColorNotActive = color(bgColor);
      //tutorialsButton.textColorNotActive = color(bgColor);


    } else if(colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
      // controlPanelCollapser.setColorNotPressed(color(150));
      // issuesButton.setColorNotPressed(color(150));
      // shopButton.setColorNotPressed(color(150));
      // tutorialsButton.setColorNotPressed(color(150));

      // controlPanelCollapser.setColorNotPressed(bgColor);
      // issuesButton.setColorNotPressed(bgColor);
      // shopButton.setColorNotPressed(bgColor);
      // tutorialsButton.setColorNotPressed(bgColor);

      controlPanelCollapser.setColorNotPressed(openbciBlue);
      //issuesButton.setColorNotPressed(openbciBlue);
      //shopButton.setColorNotPressed(openbciBlue);
      //tutorialsButton.setColorNotPressed(openbciBlue);

      controlPanelCollapser.textColorNotActive = color(255);
      //issuesButton.textColorNotActive = color(255);
      //shopButton.textColorNotActive = color(255);
      //tutorialsButton.textColorNotActive = color(255);

      // controlPanelCollapser.textColorNotActive = color(openbciBlue);
      // issuesButton.textColorNotActive = color(openbciBlue);
      // shopButton.textColorNotActive = color(openbciBlue);
      // tutorialsButton.textColorNotActive = color(openbciBlue);
      //
      // controlPanelCollapser.textColorNotActive = color(bgColor);
      // issuesButton.textColorNotActive = color(bgColor);
      // shopButton.textColorNotActive = color(bgColor);
      // tutorialsButton.textColorNotActive = color(bgColor);
    }

    if(systemMode >= SYSTEMMODE_POSTINIT){
      updateSecondaryNavButtonsColor();
    }
  }

  public void updateSecondaryNavButtonsColor(){
    if(colorScheme == COLOR_SCHEME_DEFAULT){
      filtBPButton.setColorNotPressed(color(255));
      filtNotchButton.setColorNotPressed(color(255));
      layoutButton.setColorNotPressed(color(255));

      filtBPButton.textColorNotActive = color(bgColor);
      filtNotchButton.textColorNotActive = color(bgColor);
      layoutButton.textColorNotActive = color(bgColor);
    }
    else if(colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
      filtBPButton.setColorNotPressed(color(57,128,204));
      filtNotchButton.setColorNotPressed(color(57,128,204));
      layoutButton.setColorNotPressed(color(57,128,204));

      filtBPButton.textColorNotActive = color(255);
      filtNotchButton.textColorNotActive = color(255);
      layoutButton.textColorNotActive = color(255);
    }

  }

  public void update(){
    if(systemMode >= SYSTEMMODE_POSTINIT){
      layoutSelector.update();
      tutorialSelector.update();
    }
  }

  public void draw(){
    pushStyle();

    if(colorScheme == COLOR_SCHEME_DEFAULT){
      noStroke();
      fill(229);
      rect(0, 0, width, topNav_h);
      stroke(bgColor);
      fill(255);
      rect(-1, 0, width+2, navBarHeight);
      //image(logo_blue, width/2 - (128/2) - 2, 6, 128, 22);
    } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
      noStroke();
      fill(100);
      fill(57,128,204);
      rect(0, 0, width, topNav_h);
      stroke(bgColor);
      fill(31,69,110);
      rect(-1, 0, width+2, navBarHeight);
      //image(logo_white, width/2 - (128/2) - 2, 0, 168, 36);
      textFont(h3, 16);
      
    textLeading(24);
    fill(255, 255, 255, 255);
    textAlign(CENTER, CENTER);
    //text("OpenBCI GUI v2.1.2\nJanuary 2017", width/2, height/2 + width/9);
    text("Darwin Ecosystem Augmented Cognition ",  width/2 - (128/2) - 200, -5, 500, 36);
    }

    // if(colorScheme == COLOR_SCHEME_DEFAULT){
    //
    // } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
    //
    // }

    popStyle();

    if(systemMode == SYSTEMMODE_POSTINIT){
      stopButton.draw();
      filtBPButton.draw();
      filtNotchButton.draw();
      layoutButton.draw();
    }

    controlPanelCollapser.draw();
    //tutorialsButton.draw();
    //issuesButton.draw();
    //shopButton.draw();

    // image(logo_blue, width/2 - (128/2) - 2, 6, 128, 22);


    layoutSelector.draw();
    tutorialSelector.draw();

  }

  public void screenHasBeenResized(int _x, int _y){
    //tutorialsButton.but_x = width - 3 - tutorialsButton.but_dx;
    //issuesButton.but_x = width - 3*2 - issuesButton.but_dx - tutorialsButton.but_dx;
    //shopButton.but_x = width - 3*3 - shopButton.but_dx - issuesButton.but_dx - tutorialsButton.but_dx;

    if(systemMode == SYSTEMMODE_POSTINIT){
      layoutButton.but_x = width - 3 - layoutButton.but_dx;
      layoutSelector.screenResized();     //pass screenResized along to layoutSelector
      //tutorialSelector.screenResized();
    }
  }

  public void mousePressed(){
    if(systemMode >= SYSTEMMODE_POSTINIT){
      if (stopButton.isMouseHere()) {
        stopButton.setIsActive(true);
        stopButtonWasPressed();
      }
      if (filtBPButton.isMouseHere()) {
        filtBPButton.setIsActive(true);
        incrementFilterConfiguration();
      }
      if (topNav.filtNotchButton.isMouseHere()) {
        filtNotchButton.setIsActive(true);
        incrementNotchConfiguration();
      }
      if (layoutButton.isMouseHere()) {
        layoutButton.setIsActive(true);
        //toggle layout window to enable the selection of your container layoutButton...
      }
    }

    //was control panel button pushed
    if (controlPanelCollapser.isMouseHere()) {
      if (controlPanelCollapser.isActive && systemMode == SYSTEMMODE_POSTINIT) {
        controlPanelCollapser.setIsActive(false);
        controlPanel.close();
      } else {
        controlPanelCollapser.setIsActive(true);
        // controlPanelCollapser.setIsActive(false);
        controlPanel.open();
      }
    }
    else {
      if (controlPanel.isOpen) {
        controlPanel.CPmousePressed();
      }
    }

    //this is super hacky... but needs to be done otherwise... the controlPanelCollapser doesn't match the open control panel
    if(controlPanel.isOpen){
      controlPanelCollapser.setIsActive(true);
    }

    //if (tutorialsButton.isMouseHere()) {
    //  tutorialsButton.setIsActive(true);
    //  //toggle help/tutorial dropdown menu
    //}
    //if (issuesButton.isMouseHere()) {
    //  issuesButton.setIsActive(true);
    //  //toggle help/tutorial dropdown menu
    //}
    //if (shopButton.isMouseHere()) {
    //  shopButton.setIsActive(true);
    //  //toggle help/tutorial dropdown menu
    //}

    layoutSelector.mousePressed();     //pass mousePressed along to layoutSelector
    tutorialSelector.mousePressed();
  }

  public void mouseReleased(){

    //if (tutorialsButton.isMouseHere() && tutorialsButton.isActive()) {
    //  tutorialSelector.toggleVisibility();
    //  tutorialsButton.setIsActive(true);
    //}

    //if (issuesButton.isMouseHere() && issuesButton.isActive()) {
    //  //go to Github issues
    //  issuesButton.goToURL();
    //}

    //if (shopButton.isMouseHere() && shopButton.isActive()) {
    //  //go to OpenBCI Shop
    //  shopButton.goToURL();
    //}



    if(systemMode == SYSTEMMODE_POSTINIT){

      if(!tutorialSelector.isVisible){ //make sure that you can't open the layout selector accidentally
        if (layoutButton.isMouseHere() && layoutButton.isActive()) {
          layoutSelector.toggleVisibility();
          layoutButton.setIsActive(true);
          wm.printLayouts();
        }
      }

      stopButton.setIsActive(false);
      filtBPButton.setIsActive(false);
      filtNotchButton.setIsActive(false);
      layoutButton.setIsActive(false);
    }

    //tutorialsButton.setIsActive(false);
    //issuesButton.setIsActive(false);
    //shopButton.setIsActive(false);

    layoutSelector.mouseReleased();    //pass mouseReleased along to layoutSelector
    tutorialSelector.mouseReleased();
  }

}

//=============== OLD STUFF FROM Gui_Manger.pde ===============//

public void incrementFilterConfiguration() {
  dataProcessing.incrementFilterConfiguration();

  //update the button strings
  topNav.filtBPButton.but_txt = "BP Filt\n" + dataProcessing.getShortFilterDescription();
  // topNav.titleMontage.string = "EEG Data (" + dataProcessing.getFilterDescription() + ")";
}

public void incrementNotchConfiguration() {
  dataProcessing.incrementNotchConfiguration();

  //update the button strings
  topNav.filtNotchButton.but_txt = "Notch\n" + dataProcessing.getShortNotchDescription();
  // topNav.titleMontage.string = "EEG Data (" + dataProcessing.getFilterDescription() + ")";
}

class LayoutSelector{

  int x, y, w, h, margin, b_w, b_h;
  boolean isVisible;

  ArrayList<Button> layoutOptions; //

  LayoutSelector(){
    w = 180;
    x = width - w - 3;
    y = (navBarHeight * 2) - 3;
    margin = 6;
    b_w = (w - 5*margin)/4;
    b_h = b_w;
    h = margin*3 + b_h*2;


    isVisible = false;

    layoutOptions = new ArrayList<Button>();
    addLayoutOptionButton();
  }

  public void update(){
    if(isVisible){ //only update if visible
      // //close dropdown when mouse leaves
      // if((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.layoutButton.isMouseHere()){
      //   toggleVisibility();
      // }
    }
  }

  public void draw(){
    if(isVisible){ //only draw if visible
      pushStyle();

      // println("it's happening");
      stroke(bgColor);
      // fill(229); //bg
      fill(57,128,204); //bg
      rect(x, y, w, h);

      for(int i = 0; i < layoutOptions.size(); i++){
        layoutOptions.get(i).draw();
      }

      fill(57,128,204);
      // fill(177, 184, 193);
      noStroke();
      rect(x+w-(topNav.layoutButton.but_dx-1), y, (topNav.layoutButton.but_dx-1), 1);

      popStyle();
    }
  }

  public void isMouseHere(){

  }

  public void mousePressed(){
    //only allow button interactivity if isVisible==true
    if(isVisible){
      for(int i = 0; i < layoutOptions.size(); i++){
        if(layoutOptions.get(i).isMouseHere()){
          layoutOptions.get(i).setIsActive(true);
        }
      }
    }
  }

  public void mouseReleased(){
    //only allow button interactivity if isVisible==true
    if(isVisible){
      if((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.layoutButton.isMouseHere()){
        toggleVisibility();
      }
      for(int i = 0; i < layoutOptions.size(); i++){
        if(layoutOptions.get(i).isMouseHere() && layoutOptions.get(i).isActive()){
          int layoutSelected = i+1;
          println("Layout [" + layoutSelected + "] selected.");
          output("Layout [" + layoutSelected + "] selected.");
          layoutOptions.get(i).setIsActive(false);
          toggleVisibility(); //shut layoutSelector if something is selected
          wm.setNewContainerLayout(layoutSelected-1); //have WidgetManager update Layout and active widgets
        }
      }
    }
  }

  public void screenResized(){
    //update position of outer box and buttons
    int oldX = x;
    x = width - w - 3;
    int dx = oldX - x;
    for(int i = 0; i < layoutOptions.size(); i++){
      layoutOptions.get(i).setX(layoutOptions.get(i).but_x - dx);
    }

  }

  public void toggleVisibility(){
    isVisible = !isVisible;
    if(isVisible){
      //the very convoluted way of locking all controllers of a single controlP5 instance...
      for(int i = 0; i < wm.widgets.size(); i++){
        for(int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++){
          wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).lock();
        }
      }

    }else{
      //the very convoluted way of unlocking all controllers of a single controlP5 instance...
      for(int i = 0; i < wm.widgets.size(); i++){
        for(int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++){
          wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).unlock();
        }
      }
    }
  }

  public void addLayoutOptionButton(){

    //FIRST ROW

    //setup button 1 -- full screen
    Button tempLayoutButton = new Button(x + margin, y + margin, b_w, b_h, "N/A");
    PImage tempBackgroundImage = loadImage("layout_buttons/layout_1.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 2 -- 2x2
    tempLayoutButton = new Button(x + 2*margin + b_w*1, y + margin, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_2.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 3 -- 2x1
    tempLayoutButton = new Button(x + 3*margin + b_w*2, y + margin, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_3.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 4 -- 1x2
    tempLayoutButton = new Button(x + 4*margin + b_w*3, y + margin, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_4.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //SECOND ROW

    //setup button 5
    tempLayoutButton = new Button(x + margin, y + 2*margin + 1*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_5.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 6
    tempLayoutButton = new Button(x + 2*margin + b_w*1, y + 2*margin + 1*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_6.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 7
    tempLayoutButton = new Button(x + 3*margin + b_w*2, y + 2*margin + 1*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_7.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 8
    tempLayoutButton = new Button(x + 4*margin + b_w*3, y + 2*margin + 1*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_8.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //THIRD ROW -- commented until more widgets are added

    h = margin*4 + b_h*3;
    //setup button 9
    tempLayoutButton = new Button(x + margin, y + 3*margin + 2*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_9.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 10
    tempLayoutButton = new Button(x + 2*margin + b_w*1, y + 3*margin + 2*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_10.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 11
    tempLayoutButton = new Button(x + 3*margin + b_w*2, y + 3*margin + 2*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_11.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

    //setup button 12
    tempLayoutButton = new Button(x + 4*margin + b_w*3, y + 3*margin + 2*b_h, b_w, b_h, "N/A");
    tempBackgroundImage = loadImage("layout_buttons/layout_12.png");
    tempLayoutButton.setBackgroundImage(tempBackgroundImage);
    layoutOptions.add(tempLayoutButton);

  }

  public void updateLayoutOptionButtons(){

  }

}

class TutorialSelector{

  int x, y, w, h, margin, b_w, b_h;
  boolean isVisible;

  ArrayList<Button> tutorialOptions; //

  TutorialSelector(){
    w = 180;
    x = width - w - 3;
    y = (navBarHeight) - 3;
    margin = 6;
    b_w = w - margin*2;
    b_h = 22;
    h = margin*3 + b_h*2;


    isVisible = false;

    tutorialOptions = new ArrayList<Button>();
    addTutorialButtons();
  }

  public void update(){
    if(isVisible){ //only update if visible
      // //close dropdown when mouse leaves
      // if((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.tutorialsButton.isMouseHere()){
      //   toggleVisibility();
      // }
    }
  }

  public void draw(){
    if(isVisible){ //only draw if visible
      pushStyle();

      // println("it's happening");
      stroke(bgColor);
      // fill(229); //bg
      fill(31,69,110); //bg
      rect(x, y, w, h);

      for(int i = 0; i < tutorialOptions.size(); i++){
        tutorialOptions.get(i).draw();
      }

      fill(openbciBlue);
      // fill(177, 184, 193);
      noStroke();
      rect(x+w-(topNav.tutorialsButton.but_dx-1), y, (topNav.tutorialsButton.but_dx-1) , 1);

      popStyle();
    }
  }

  public void isMouseHere(){

  }

  public void mousePressed(){
    //only allow button interactivity if isVisible==true
    if(isVisible){
      for(int i = 0; i < tutorialOptions.size(); i++){
        if(tutorialOptions.get(i).isMouseHere()){
          tutorialOptions.get(i).setIsActive(true);
        }
      }
    }
  }

  public void mouseReleased(){
    //only allow button interactivity if isVisible==true
    if(isVisible){
      if((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.tutorialsButton.isMouseHere()){
        toggleVisibility();
      }
      for(int i = 0; i < tutorialOptions.size(); i++){
        if(tutorialOptions.get(i).isMouseHere() && tutorialOptions.get(i).isActive()){
          int tutorialSelected = i+1;
          tutorialOptions.get(i).setIsActive(false);
          tutorialOptions.get(i).goToURL();
          println("Attempting to use your default web browser to open " + tutorialOptions.get(i).myURL);
          output("Layout [" + tutorialSelected + "] selected.");
          toggleVisibility(); //shut layoutSelector if something is selected
          //open corresponding link
        }
      }
    }
  }

  public void screenResized(){
    //update position of outer box and buttons
    int oldX = x;
    x = width - w - 3;
    int dx = oldX - x;
    for(int i = 0; i < tutorialOptions.size(); i++){
      tutorialOptions.get(i).setX(tutorialOptions.get(i).but_x - dx);
    }

  }

  public void toggleVisibility(){
    isVisible = !isVisible;
    if(systemMode >= SYSTEMMODE_POSTINIT){
      if(isVisible) {
        //the very convoluted way of locking all controllers of a single controlP5 instance...
        for(int i = 0; i < wm.widgets.size(); i++){
          for(int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++){
            wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).lock();
          }
        }

      } else {
        //the very convoluted way of unlocking all controllers of a single controlP5 instance...
        for(int i = 0; i < wm.widgets.size(); i++) {
          for(int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
            wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).unlock();
          }
        }
      }
    }
  }

  public void addTutorialButtons(){

    //FIRST ROW

    //setup button 1 -- full screen
    int buttonNumber = 0;
    Button tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "Getting Started");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://docs.openbci.com/");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 1;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "Testing Impedance");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://docs.openbci.com/hardware/01-OpenBCI_Hardware");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 2;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "OpenBCI Forum");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://openbci.com/index.php/forum/");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 3;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "Building Widgets");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://docs.openbci.com/software/01-OpenBCI_SDK");
    tutorialOptions.add(tempTutorialButton);

  }

  public void updateLayoutOptionButtons(){

  }

}
/////////////////////////////////////////////////////////////////////////////////
//
//  Emg_Widget is used to visiualze EMG data by channel, and to trip events
//
//  Created: Colin Fausnaught, December 2016 (with a lot of reworked code from Tao)
//
//  Custom widget to visiualze EMG data. Features dragable thresholds, serial
//  out communication, channel configuration, digital and analog events.
//
//  KNOWN ISSUES: Cannot resize with window dragging events
//
//  TODO: Add dynamic threshold functionality
////////////////////////////////////////////////////////////////////////////////

// addDropdown("SmoothEMG", "Smooth", Arrays.asList("0.01 s", "0.1 s", "0.15 s", "0.25 s", "0.5 s", "1.0 s", "2.0 s"), 0);
// addDropdown("uVLimit", "uV Limit", Arrays.asList("50 uV", "100 uV", "200 uV", "400 uV"), 0);
// addDropdown("CreepSpeed", "Creep", Arrays.asList("0.9", "0.95", "0.98", "0.99", "0.999"), 0);
// addDropdown("minUVRange", "Min \u0394uV", Arrays.asList("10 uV", "20 uV", "40 uV", "80 uV"), 0);
//
// int averagePeriod = 125;          //number of data packets to average over (250 = 1 sec)
// float acceptableLimitUV = 200.0;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
// float creepSpeed = 0.99;
// float minRange = 20.0;

public void SmoothEMG(int n){

  float samplesPerSecond;
  if(eegDataSource == DATASOURCE_GANGLION){
    samplesPerSecond = 200;
  } else {
    samplesPerSecond = 250;
  }

  for(int i = 0 ; i < w_emg.motorWidgets.length; i++){
    if(n == 0){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.01f;
    }
    if(n == 1){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.1f;
    }
    if(n == 2){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.15f;
    }
    if(n == 3){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.25f;
    }
    if(n == 4){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.5f;
    }
    if(n == 5){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 0.75f;
    }
    if(n == 6){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 1.0f;
    }
    if(n == 7){
      w_emg.motorWidgets[i].averagePeriod = samplesPerSecond * 2.0f;
    }
  }
  closeAllDropdowns();
}

public void uVLimit(int n){
  for(int i = 0 ; i < w_emg.motorWidgets.length; i++){
    if(n == 0){
      w_emg.motorWidgets[i].acceptableLimitUV = 50.0f;
    }
    if(n == 1){
      w_emg.motorWidgets[i].acceptableLimitUV = 100.0f;
    }
    if(n == 2){
      w_emg.motorWidgets[i].acceptableLimitUV = 200.0f;
    }
    if(n == 3){
      w_emg.motorWidgets[i].acceptableLimitUV = 400.0f;
    }
  }
  closeAllDropdowns();
}

public void CreepSpeed(int n){
  for(int i = 0 ; i < w_emg.motorWidgets.length; i++){
    if(n == 0){
      w_emg.motorWidgets[i].creepSpeed = 0.9f;
    }
    if(n == 1){
      w_emg.motorWidgets[i].creepSpeed = 0.95f;
    }
    if(n == 2){
      w_emg.motorWidgets[i].creepSpeed = 0.98f;
    }
    if(n == 3){
      w_emg.motorWidgets[i].creepSpeed = 0.99f;
    }
    if(n == 4){
      w_emg.motorWidgets[i].creepSpeed = 0.999f;
    }
  }
  closeAllDropdowns();
}

public void minUVRange(int n){
  for(int i = 0 ; i < w_emg.motorWidgets.length; i++){
    if(n == 0){
      w_emg.motorWidgets[i].minRange = 10.0f;
    }
    if(n == 1){
      w_emg.motorWidgets[i].minRange = 20.0f;
    }
    if(n == 2){
      w_emg.motorWidgets[i].minRange = 40.0f;
    }
    if(n == 3){
      w_emg.motorWidgets[i].minRange = 80.0f;
    }
    if(n == 4){
      w_emg.motorWidgets[i].minRange = 5.0f;
    }
  }
  closeAllDropdowns();
}

class W_emg extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  Motor_Widget[] motorWidgets;
  TripSlider[] tripSliders;
  TripSlider[] untripSliders;
  List<String> baudList;
  List<String> serList;
  List<String> channelList;
  boolean[] events;
  int currChannel;
  int theBaud;
  Button connectButton;
  Serial serialOutEMG;
  String theSerial;

  Boolean emgAdvanced = false;

  PApplet parent;

  W_emg (PApplet _parent) {
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
    parent = _parent;

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function

    //use these as new configuration widget
    motorWidgets = new Motor_Widget[nchan];

    for (int i = 0; i < nchan; i++) {
      motorWidgets[i] = new Motor_Widget();
      motorWidgets[i].ourChan = i;
      if(eegDataSource == DATASOURCE_GANGLION){
        motorWidgets[i].averagePeriod = 200 * 0.5f;
      } else {
        motorWidgets[i].averagePeriod = 250 * 0.5f;
      }
    }

    events = new boolean[nchan];

    for (int i = 0; i < nchan; i++) {
      events[i] = true;
    }

    addDropdown("SmoothEMG", "Smooth", Arrays.asList("0.01 s", "0.1 s", "0.15 s", "0.25 s", "0.5 s", "0.75 s", "1.0 s", "2.0 s"), 4);
    addDropdown("uVLimit", "uV Limit", Arrays.asList("50 uV", "100 uV", "200 uV", "400 uV"), 2);
    addDropdown("CreepSpeed", "Creep", Arrays.asList("0.9", "0.95", "0.98", "0.99", "0.999"), 3);
    addDropdown("minUVRange", "Min \u0394uV", Arrays.asList("10 uV", "20 uV", "40 uV", "80 uV", "5 uV"), 1);

    if (emgAdvanced) {
      channelList = new ArrayList<String>();
      baudList = new ArrayList<String>();
      serList = new ArrayList<String>();
      for (int i = 0; i < nchan; i++) {
        channelList.add(Integer.toString(i + 1));
      }

      currChannel = 0;
      theBaud = 230400;

      baudList.add("NONE");
      baudList.add(Integer.toString(230400));
      baudList.add(Integer.toString(115200));
      baudList.add(Integer.toString(57600));
      baudList.add(Integer.toString(38400));
      baudList.add(Integer.toString(28800));
      baudList.add(Integer.toString(19200));
      baudList.add(Integer.toString(14400));
      baudList.add(Integer.toString(9600));
      baudList.add(Integer.toString(7200));
      baudList.add(Integer.toString(4800));
      baudList.add(Integer.toString(3600));
      // // ignore below here... I don't think these baud rates will be necessary
      // baudList.add(Integer.toString(2400));
      // baudList.add(Integer.toString(1800));
      // baudList.add(Integer.toString(1200));
      // baudList.add(Integer.toString(600));
      // baudList.add(Integer.toString(300));

      String[] serialPorts = Serial.list();
      serList.add("NONE");
      for (int i = 0; i < serialPorts.length; i++) {
        String tempPort = serialPorts[(serialPorts.length - 1) - i];
        if (!tempPort.equals(openBCI_portName)) serList.add(tempPort);
      }

      addDropdown("SerialSelection", "Output", serList, 0);
      addDropdown("ChannelSelection", "Channel", channelList, 0);
      addDropdown("EventType", "Event Type", Arrays.asList("Digital", "Analog"), 0);
      addDropdown("BaudRate", "Baud Rate", baudList, 0);
      tripSliders = new TripSlider[nchan];
      untripSliders = new TripSlider[nchan];

      initSliders(w, h);
    }
  }

  //Initalizes the threshold
  public void initSliders(int rw, int rh) {
    //Stole some logic from the rectangle drawing in draw()
    int rowNum = 4;
    int colNum = motorWidgets.length / rowNum;
    int index = 0;

    float rowOffset = rh / rowNum;
    float colOffset = rw / colNum;

    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {

        println("ROW: " + (4*rowOffset/8));
        tripSliders[index] = new TripSlider(PApplet.parseInt((5*colOffset/8) * 0.498f), PApplet.parseInt((2 * rowOffset / 8) * 0.384f), (4*rowOffset/8) * 0.408f, PApplet.parseInt((3*colOffset/32) * 0.489f), 2, tripSliders, true, motorWidgets[index]);
        untripSliders[index] = new TripSlider(PApplet.parseInt((5*colOffset/8) * 0.498f), PApplet.parseInt((2 * rowOffset / 8) * 0.384f), (4*rowOffset/8) * 0.408f, PApplet.parseInt((3*colOffset/32) * 0.489f), 2, tripSliders, false, motorWidgets[index]);
        //println("Slider :" + (j+i) + " first: " + int((5*colOffset/8) * 0.498)+ " second: " + int((2 * rowOffset / 8) * 0.384) + " third: " + int((3*colOffset/32) * 0.489));
        tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
        untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
        index++;
      }
    }
  }

  public void update() {
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff);
  }

  public void draw() {
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();
    noStroke();
    fill(255);
    rect(x, y, w, h);

    if (emgAdvanced) {
      if (connectButton != null) connectButton.draw();
      else connectButton = new Button(PApplet.parseInt(x) + 2, PApplet.parseInt(y) - navHeight + 2, 100, navHeight - 6, "Connect", fontInfo.buttonLabel_size);

      stroke(1, 18, 41, 125);

      if (connectButton != null && connectButton.wasPressed) {
        fill(0, 255, 0);
        ellipse(x + 120, y - navHeight/2, 16, 16);
      } else if (connectButton != null && !connectButton.wasPressed) {
        fill(255, 0, 0);
        ellipse(x + 120, y - navHeight/2, 16, 16);
      }
    }


    // float rx = x, ry = y + 2* navHeight, rw = w, rh = h - 2*navHeight;
    float rx = x, ry = y, rw = w, rh = h;
    float scaleFactor = 1.0f;
    float scaleFactorJaw = 1.5f;
    int rowNum = 4;
    int colNum = motorWidgets.length / rowNum;
    float rowOffset = rh / rowNum;
    float colOffset = rw / colNum;
    int index = 0;
    float currx, curry;
    //new
    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {
        
        if (isRunning){
          float outputVal = motorWidgets[i * colNum + j].myAverage;
            int tester = i * colNum + j +1;
            //println(tester + ": " + outputVal);
          
        }
        
        //%%%%%
        pushMatrix();
        currx = rx + j * colOffset;
        curry = ry + i * rowOffset; //never name variables on an empty stomach
        translate(currx, curry);

        //draw visualizer

        // (int)color(129, 129, 129),
        // (int)color(124, 75, 141),
        // (int)color(54, 87, 158),
        // (int)color(49, 113, 89),
        // (int)color(221, 178, 13),
        // (int)color(253, 94, 52),
        // (int)color(224, 56, 45),
        // (int)color(162, 82, 49),

        //realtime
        // fill(255, 0, 0, 125);
        fill(red(channelColors[index%8]), green(channelColors[index%8]), blue(channelColors[index%8]), 200);
        noStroke();
        ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].myAverage, scaleFactor * motorWidgets[i * colNum + j].myAverage);
        
        //circle for outer threshold
        // stroke(0, 255, 0);
        noFill();
        strokeWeight(1);
        stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].upperThreshold, scaleFactor * motorWidgets[i * colNum + j].upperThreshold);

        //circle for inner threshold
        // stroke(0, 255, 255);
        stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        ellipse(2*colOffset/8, rowOffset / 2, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold, scaleFactor * motorWidgets[i * colNum + j].lowerThreshold);

        int _x = PApplet.parseInt(5*colOffset/8);
        int _y = PApplet.parseInt(2 * rowOffset / 8);
        int _w = PApplet.parseInt(5*colOffset/32);
        int _h = PApplet.parseInt(4*rowOffset/8);

        //draw normalized bar graph of uV w/ matching channel color
        noStroke();
        fill(red(channelColors[index%8]), green(channelColors[index%8]), blue(channelColors[index%8]), 200);
        rect(_x, 3*_y + 1, _w, map(motorWidgets[i * colNum + j].output_normalized, 0, 1, 0, (-1) * PApplet.parseInt((4*rowOffset/8))));

        //draw background bar container for mapped uV value indication
        strokeWeight(1);
        stroke(red(bgColor), green(bgColor), blue(bgColor), 150);
        noFill();
        rect(_x, _y, _w, _h);

        //draw trip & untrip threshold bars
        if (emgAdvanced) {
          tripSliders[index].update(currx, curry);
          tripSliders[index].display(_x, _y, _w, _h);
          untripSliders[index].update(currx, curry);
          untripSliders[index].display(_x, _y, _w, _h);
        }

        //draw channel number at upper left corner of row/column cell
        pushStyle();
        stroke(0);
        fill(bgColor);
        int _chan = index+1;
        textFont(p5, 12);
        text(_chan + "", 10, 20);
        // rectMode(CORNERS);
        // rect(0, 0, 10, 10);
        popStyle();

        index++;
        popMatrix();
      }
    }

    popStyle();
  }

  public void screenResized() {
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    //widgetTemplateButton.setPos(x + w/2 - widgetTemplateButton.but_dx/2, y + h/2 - widgetTemplateButton.but_dy/2);
    if (emgAdvanced) {
      connectButton.setPos(PApplet.parseInt(x) + 2, PApplet.parseInt(y) - navHeight + 2);

      for (int i = 0; i < tripSliders.length; i++) {
        //update slider positions
      }
    }
  }

  public void mousePressed() {
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (emgAdvanced) {
      if (connectButton.isMouseHere()) {
        connectButton.setIsActive(true);
        println("Connect pressed");
      } else connectButton.setIsActive(false);
    }
  }

  public void mouseReleased() {
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    if (emgAdvanced) {
      if (connectButton != null && connectButton.isMouseHere()) {
        //do some function

        try {
          serialOutEMG = new Serial(parent, theSerial, theBaud);
          connectButton.wasPressed = true;
          verbosePrint("Connected");
          output("Connected to " + theSerial);
        }
        catch (Exception e) {
          connectButton.wasPressed = false;
          verbosePrint("Could not connect!");
          output("Could not connect. Confirm that your Serial/COM port is correct and active.");
        }

        connectButton.setIsActive(false);
      }

      for (int i = 0; i<nchan; i++) {
        tripSliders[i].releaseEvent();
        untripSliders[i].releaseEvent();
      }
    }
  }


  public void process(float[][] data_newest_uV, //holds raw EEG data that is new since the last call
    float[][] data_long_uV, //holds a longer piece of buffered EEG data, of same length as will be plotted on the screen
    float[][] data_forDisplay_uV, //this data has been filtered and is ready for plotting on the screen
    FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data

    //for example, you could loop over each EEG channel to do some sort of time-domain processing
    //using the sample values that have already been filtered, as will be plotted on the display
    //float EEG_value_uV;

    //looping over channels and analyzing input data
    for (Motor_Widget cfc : motorWidgets) {
      cfc.myAverage = 0.0f;
      for (int i = data_forDisplay_uV[cfc.ourChan].length - PApplet.parseInt(cfc.averagePeriod); i < data_forDisplay_uV[cfc.ourChan].length; i++) {
        if (abs(data_forDisplay_uV[cfc.ourChan][i]) <= cfc.acceptableLimitUV) { //prevent BIG spikes from effecting the average
          cfc.myAverage += abs(data_forDisplay_uV[cfc.ourChan][i]);  //add value to average ... we will soon divide by # of packets
        } else {
          cfc.myAverage += cfc.acceptableLimitUV; //if it's greater than the limit, just add the limit
        }
      }
      cfc.myAverage = cfc.myAverage / cfc.averagePeriod; // float(cfc.averagePeriod); //finishing the average

      if (cfc.myAverage >= cfc.upperThreshold && cfc.myAverage <= cfc.acceptableLimitUV) { //
        cfc.upperThreshold = cfc.myAverage;
      }
      if (cfc.myAverage <= cfc.lowerThreshold) {
        cfc.lowerThreshold = cfc.myAverage;
      }
      if (cfc.upperThreshold >= (cfc.myAverage + cfc.minRange)) {  //minRange = 15
        cfc.upperThreshold *= cfc.creepSpeed; //adjustmentSpeed
      }
      if (cfc.lowerThreshold <= 1){
        cfc.lowerThreshold = 1.0f;
      }
      if (cfc.lowerThreshold <= cfc.myAverage) {
        cfc.lowerThreshold *= (1)/(cfc.creepSpeed); //adjustmentSpeed
        // cfc.lowerThreshold += (10 - cfc.lowerThreshold)/(frameRate * 5); //have lower threshold creep upwards to keep range tight
      }
      if (cfc.upperThreshold <= (cfc.lowerThreshold + cfc.minRange)){
        cfc.upperThreshold = cfc.lowerThreshold + cfc.minRange;
      }
      // if (cfc.upperThreshold >= (cfc.myAverage + 35)) {
      //   cfc.upperThreshold *= .97;
      // }
      // if (cfc.lowerThreshold <= cfc.myAverage) {
      //   cfc.lowerThreshold += (10 - cfc.lowerThreshold)/(frameRate * 5); //have lower threshold creep upwards to keep range tight
      // }
      //output_L = (int)map(myAverage_L, lowerThreshold_L, upperThreshold_L, 0, 255);
      cfc.output_normalized = map(cfc.myAverage, cfc.lowerThreshold, cfc.upperThreshold, 0, 1);
      if(cfc.output_normalized < 0){
        cfc.output_normalized = 0; //always make sure this value is >= 0
      }
      cfc.output_adjusted = ((-0.1f/(cfc.output_normalized*255.0f)) + 255.0f);



      //=============== TRIPPIN ==================
      //= Just calls all the trip events         =
      //==========================================

      switch(cfc.ourChan) {

      case 0:
        if (events[0]) digitalEventChan0(cfc);
        else analogEventChan0(cfc);
        break;
      case 1:
        if (events[1]) digitalEventChan1(cfc);
        else analogEventChan1(cfc);
        break;
      case 2:
        if (events[2]) digitalEventChan2(cfc);
        else analogEventChan2(cfc);
        break;
      case 3:
        if (events[3]) digitalEventChan3(cfc);
        else analogEventChan3(cfc);
        break;
      case 4:
        if (events[4]) digitalEventChan4(cfc);
        else analogEventChan4(cfc);
        break;
      case 5:
        if (events[5]) digitalEventChan5(cfc);
        else  analogEventChan5(cfc);
        break;
      case 6:
        if (events[6]) digitalEventChan6(cfc);
        else analogEventChan6(cfc);
        break;
      case 7:
        if (events[7]) digitalEventChan7(cfc);
        else analogEventChan7(cfc);
        break;
      case 8:
        if (events[8]) digitalEventChan8(cfc);
        else analogEventChan8(cfc);
        break;
      case 9:
        if (events[9]) digitalEventChan9(cfc);
        else analogEventChan9(cfc);
        break;
      case 10:
        if (events[10]) digitalEventChan10(cfc);
        else analogEventChan10(cfc);
        break;
      case 11:
        if (events[11]) digitalEventChan11(cfc);
        else analogEventChan11(cfc);
        break;
      case 12:
        if (events[12]) digitalEventChan12(cfc);
        else analogEventChan12(cfc);
        break;
      case 13:
        if (events[13]) digitalEventChan13(cfc);
        else analogEventChan13(cfc);
        break;
      case 14:
        if (events[14]) digitalEventChan14(cfc);
        else analogEventChan14(cfc);
        break;
      case 15:
        if (events[15]) digitalEventChan15(cfc);
        else analogEventChan15(cfc);
        break;
      default:
        break;
      }
    }
    //=================== OpenBionics switch example ==============================

    if (millis() - motorWidgets[0].timeOfLastTrip >= 2000 && serialOutEMG != null) {
      //println("Counter: " + motorWidgets[0].switchCounter);
      switch(motorWidgets[0].switchCounter) {
      case 1:
        serialOutEMG.write("G0");
       println("OVER");
        break;
      }
      motorWidgets[0].switchCounter = 0;
    }

    //----------------- Leftover from Tou Code, what does this do? ----------------------------
    //OR, you could loop over each EEG channel and do some sort of frequency-domain processing from the FFT data
    float FFT_freq_Hz, FFT_value_uV;
    for (int Ichan=0; Ichan < nchan; Ichan++) {
      //loop over each new sample
      for (int Ibin=0; Ibin < fftBuff[Ichan].specSize(); Ibin++) {
        FFT_freq_Hz = fftData[Ichan].indexToFreq(Ibin);
        FFT_value_uV = fftData[Ichan].getBand(Ibin);

        //add your processing here...
      }
    }
    //---------------------------------------------------------------------------------
  }

  class Motor_Widget {
    //variables
    boolean isTriggered = false;
    float upperThreshold = 25;        //default uV upper threshold value ... this will automatically change over time
    float lowerThreshold = 0;         //default uV lower threshold value ... this will automatically change over time
    int thresholdPeriod = 1250;       //number of packets
    int ourChan = 0;                  //channel being monitored ... "3 - 1" means channel 3 (with a 0 index)
    float myAverage = 0.0f;            //this will change over time ... used for calculations below
    //prez related
    boolean switchTripped = false;
    int switchCounter = 0;
    float timeOfLastTrip = 0;
    float tripThreshold = 0.75f;
    float untripThreshold = 0.5f;
    //if writing to a serial port
    int output = 0;                   //value between 0-255 that is the relative position of the current uV average between the rolling lower and upper uV thresholds
    float output_normalized = 0;      //converted to between 0-1
    float output_adjusted = 0;        //adjusted depending on range that is expected on the other end, ie 0-255?
    boolean analogBool = true;        //Analog events?
    boolean digitalBool = true;       //Digital events?

    //these are the 4 variables affected by the dropdown menus
    float averagePeriod; // = 125;          //number of data packets to average over (250 = 1 sec)
    float acceptableLimitUV = 200.0f;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
    float creepSpeed = 0.99f;
    float minRange = 20.0f;

  };

  //============= TripSlider =============
  //=  Class for moving thresholds. Can  =
  //=  be dragged up and down, but lower =
  //=  thresholds cannot go above upper  =
  //=  thresholds (and visa versa).      =
  //======================================
  class TripSlider {
    //Fields
    int lx, ly;
    int boxx, boxy;
    int stretch;
    int wid;
    int len;
    int boxLen;
    boolean over;
    boolean press;
    boolean locked = false;
    boolean otherslocked = false;
    boolean trip;
    boolean drawHand;
    TripSlider[] others;
    int current_color = color(255, 255, 255);
    Motor_Widget parent;

    //Constructor
    TripSlider(int ix, int iy, float il, int iwid, int ilen, TripSlider[] o, boolean wastrip, Motor_Widget p) {
      lx = ix;
      ly = iy;
      boxLen = PApplet.parseInt(il);
      wid = iwid;
      len = ilen;
      boxx = lx - wid/2;
      //boxx = lx;
      boxy = ly-stretch - len/2;
      //boxy = ly;
      others = o;
      trip = wastrip;  //Boolean to distinguish between trip and untrip thresholds
      parent = p;
      //boxLen = 31;
    }

    //Called whenever thresholds are dragged
    public void update(float tx, float ty) {
      // println("testing...");
      boxx = lx;
      //boxy = (wid + (ly/2)) - int(((wid + (ly/2)) - ly) * (float(stretch) / float(wid)));
      //boxy = ly + (ly - int( ly * (float(stretch) / float(wid)))) ;
      boxy = PApplet.parseInt(ly + stretch); //- stretch;

      for (int i=0; i<others.length; i++) {
        if (others[i].locked == true) {
          otherslocked = true;
          break;
        } else {
          otherslocked = false;
        }
      }

      if (otherslocked == false) {
        overEvent(tx, ty);
        pressEvent();
      }

      if (press) {
        //Some of this may need to be refactored in order to support window resizing
        // int mappedVal = int(mouseY - (ty + ly));
        // //int mappedVal = int(map((mouseY - (ty + ly) ), ((ty+ly) + wid - (ly/2)) - (ty+ly), 0, 0, wid));
        int mappedVal = PApplet.parseInt(mouseY - (ty+ly));

        //println("bxLen: " + boxLen + " ty: " + ty + " ly: " + ly + " mouseY: " + mouseY + " boxy: " + boxy + " stretch: " + stretch + " width: " + wid + " mappedVal: " + mappedVal);

        if (!trip) stretch = lock(mappedVal, PApplet.parseInt(parent.untripThreshold * (boxLen)), boxLen);
        else stretch =  lock(mappedVal, 0, PApplet.parseInt(parent.tripThreshold * (boxLen)));

        if (mappedVal > boxLen && !trip) parent.tripThreshold = 1;
        else if (mappedVal > boxLen && trip) parent.untripThreshold = 1;
        else if (mappedVal < 0 && !trip) parent.tripThreshold = 0;
        else if (mappedVal < 0 && trip) parent.untripThreshold = 0;
        else if (!trip) parent.tripThreshold = PApplet.parseFloat(mappedVal) / (boxLen);
        else if (trip) parent.untripThreshold = PApplet.parseFloat(mappedVal) / (boxLen);
      }
    }

    //Checks if mouse is here
    public void overEvent(float tx, float ty) {
      if (overRect(PApplet.parseInt(boxx + tx), PApplet.parseInt(boxy + ty), wid, len)) {
        over = true;
      } else {
        over = false;
      }
    }

    //Checks if mouse is pressed
    public void pressEvent() {
      if (over && mousePressed || locked) {
        press = true;
        locked = true;
      } else {
        press = false;
      }
    }

    //Mouse was released
    public void releaseEvent() {
      locked = false;
    }

    //Color selector and cursor setter
    public void setColor() {
      if (over) {
        current_color = color(127, 134, 143);
        if (!drawHand) {
          cursor(HAND);
          drawHand = true;
        }
      } else {

        if (trip) {
          current_color = color(0, 255, 0); //trip switch bar color
        } else {
          current_color = color(255, 0, 0); //untrip switch bar color
        }

        if (drawHand) {
          cursor(ARROW);
          drawHand = false;
        }
      }
    }

    //Helper function to make setting default threshold values easier.
    //Expects a float as input (0.25 is 25%)
    public void setStretchPercentage(float val) {
      stretch = lock(PApplet.parseInt(boxLen - ((boxLen) * val)), 0, boxLen);
    }

    //Displays the thresholds %%%%%
    public void display(float tx, float ty, float tw, float tl) {
      lx = PApplet.parseInt(tx);
      ly = PApplet.parseInt(ty);
      wid = PApplet.parseInt(tw);
      boxLen = PApplet.parseInt(tl);

      fill(255);
      strokeWeight(1);
      stroke(bgColor);
      setColor();
      fill(current_color);
      rect(boxx, boxy, wid, len);

      // rect(lx, ly, wid, len);
    }

    //Check if the mouse is here
    public boolean overRect(int lx, int ly, int twidth, int theight) {
      if (mouseX >= lx && mouseX <= lx+twidth &&
        mouseY >= ly && mouseY <= ly+theight) {

        return true;
      } else {
        return false;
      }
    }

    //Locks the threshold in place
    public int lock(int val, int minv, int maxv) {
      return  min(max(val, minv), maxv);
    }
  };


  //===================== DIGITAL EVENTS =============================
  //=  Digital Events work by tripping certain thresholds, and then  =
  //=  untripping said thresholds. In order to use digital events    =
  //=  you will need to observe the switchCounter field in any       =
  //=  given channel. Check out the OpenBionics Switch Example       =
  //=  in the process() function above to get an idea of how to do   =
  //=  this. It is important that your observation of switchCounter  =
  //=  is done in the process() function AFTER the Digital Events    =
  //=  are evoked.                                                   =
  //=                                                                =
  //=  This system supports both digital and analog events           =
  //=  simultaneously and seperated.                                 =
  //==================================================================

  //Channel 1 Event
  public void digitalEventChan0(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      println("tripped");
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      println("Cleared");
      cfc.switchTripped = false;
    }
  }

  //Channel 2 Event
  public void digitalEventChan1(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 3 Event
  public void digitalEventChan2(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 4 Event
  public void digitalEventChan3(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 5 Event
  public void digitalEventChan4(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 6 Event
  public void digitalEventChan5(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 7 Event
  public void digitalEventChan6(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 8 Event
  public void digitalEventChan7(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 9 Event
  public void digitalEventChan8(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 10 Event
  public void digitalEventChan9(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 11 Event
  public void digitalEventChan10(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 12 Event
  public void digitalEventChan11(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 13 Event
  public void digitalEventChan12(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 14 Event
  public void digitalEventChan13(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 15 Event
  public void digitalEventChan14(Motor_Widget cfc) {
    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }

  //Channel 16 Event
  public void digitalEventChan15(Motor_Widget cfc) {

    //Local instances of Motor_Widget fields
    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;

    //Custom waiting threshold
    int timeToWaitThresh = 750;

    if (output_normalized >= tripThreshold && !switchTripped && millis() - timeOfLastTrip >= timeToWaitThresh) {
      //Tripped
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
      cfc.switchTripped = false;
    }
  }


  //===================== ANALOG EVENTS ===========================
  //=  Analog events are a big more complicated than digital      =
  //=  events. In order to use analog events you must map the     =
  //=  output_normalized value to whatver minimum and maximum     =
  //=  you'd like and then write that to the serialOutEMG.        =
  //=                                                             =
  //=  Check out analogEventChan0() for the OpenBionics analog    =
  //=  event example to get an idea of how to use analog events.  =
  //===============================================================

  //Channel 1 Event
  public void analogEventChan0(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;


    //================= OpenBionics Analog Movement Example =======================
    if (serialOutEMG != null) {
      //println("Output normalized: " + int(map(output_normalized, 0, 1, 0, 100)));
      if (PApplet.parseInt(map(output_normalized, 0, 1, 0, 100)) > 10) {
        serialOutEMG.write("G0P" + PApplet.parseInt(map(output_normalized, 0, 1, 0, 100)));
        delay(10);
      } else serialOutEMG.write("G0P0");
    }
  }

  //Channel 2 Event
  public void analogEventChan1(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 3 Event
  public void analogEventChan2(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 4 Event
  public void analogEventChan3(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 5 Event
  public void analogEventChan4(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 6 Event
  public void analogEventChan5(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 7 Event
  public void analogEventChan6(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 8 Event
  public void analogEventChan7(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 9 Event
  public void analogEventChan8(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 10 Event
  public void analogEventChan9(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 11 Event
  public void analogEventChan10(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 12 Event
  public void analogEventChan11(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 13 Event
  public void analogEventChan12(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 14 Event
  public void analogEventChan13(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 15 Event
  public void analogEventChan14(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 16 Event
  public void analogEventChan15(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }
};


public void ChannelSelection(int n) {
  w_emg.currChannel = n;
  closeAllDropdowns();
}

public void EventType(int n) {
  if (n == 0) w_emg.events[w_emg.currChannel] = true;
  else if (n == 1) w_emg.events[w_emg.currChannel] = false;
  closeAllDropdowns();
}

public void BaudRate(int n) {
  if (!w_emg.baudList.get(n).equals("NONE")) w_emg.theBaud = Integer.parseInt(w_emg.baudList.get(n));
  closeAllDropdowns();
}

public void SerialSelection(int n) {
  if (!w_emg.serList.get(n).equals("NONE")) w_emg.theSerial = w_emg.serList.get(n);
  closeAllDropdowns();
}

////////////////////////////////////////////////////
//
// This class creates an FFT Plot separate from the old Gui_Manager
// It extends the Widget class
//
// Conor Russomanno, November 2016
//
// Requires the plotting library from grafica ... replacing the old gwoptics (which is now no longer supported)
//
///////////////////////////////////////////////////

//fft constants
int Nfft = 256; //set resolution of the FFT.  Use N=256 for normal, N=512 for MU waves
FFT[] fftBuff = new FFT[nchan];    //from the minim library
boolean isFFTFiltered = true; //yes by default ... this is used in dataProcessing.pde to determine which uV array feeds the FFT calculation

class W_fft extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde

  //put your custom variables here...
  GPlot fft_plot; //create an fft plot for each active channel
  GPointsArray[] fft_points;  //create an array of points for each channel of data (4, 8, or 16)
  int[] lineColor = {
    (int)color(129, 129, 129),
    (int)color(124, 75, 141),
    (int)color(54, 87, 158),
    (int)color(49, 113, 89),
    (int)color(221, 178, 13),
    (int)color(253, 94, 52),
    (int)color(224, 56, 45),
    (int)color(162, 82, 49),
    (int)color(129, 129, 129),
    (int)color(124, 75, 141),
    (int)color(54, 87, 158),
    (int)color(49, 113, 89),
    (int)color(221, 178, 13),
    (int)color(253, 94, 52),
    (int)color(224, 56, 45),
    (int)color(162, 82, 49)
  };

  int[] xLimOptions = {20, 40, 60, 120};
  int[] yLimOptions = {10, 50, 100, 1000};

  int xLim = xLimOptions[2];  //maximum value of x axis ... in this case 20 Hz, 40 Hz, 60 Hz, 120 Hz
  int xMax = xLimOptions[3];
  int FFT_indexLim = PApplet.parseInt(1.0f*xMax*(Nfft/get_fs_Hz_safe()));   // maxim value of FFT index
  int yLim = 100;  //maximum value of y axis ... 100 uV

  W_fft(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    addDropdown("MaxFreq", "Max Freq", Arrays.asList("20 Hz", "40 Hz", "60 Hz", "120 Hz"), 2);
    addDropdown("VertScale", "Max uV", Arrays.asList("10 uV", "50 uV", "100 uV", "1000 uV"), 2);
    addDropdown("LogLin", "Log/Lin", Arrays.asList("Log", "Linear"), 0);
    addDropdown("Smoothing", "Smooth", Arrays.asList("0.0", "0.5", "0.75", "0.9", "0.95", "0.98"), smoothFac_ind); //smoothFac_ind is a global variable at the top of W_headPlot.pde
    addDropdown("UnfiltFilt", "Filters?", Arrays.asList("Filtered", "Unfilt."), 0);

    fft_points = new GPointsArray[nchan];
    println(fft_points.length);
    initializeFFTPlot(_parent);

  }

  public void initializeFFTPlot(PApplet _parent) {
    //setup GPlot for FFT
    fft_plot =  new GPlot(_parent, x, y-navHeight, w, h+navHeight); //based on container dimensions
    fft_plot.getXAxis().setAxisLabelText("Frequency (Hz)");
    fft_plot.getYAxis().setAxisLabelText("Amplitude (uV)");
    //fft_plot.setMar(50,50,50,50); //{ bot=60, left=70, top=40, right=30 } by default
    fft_plot.setMar(60, 70, 40, 30); //{ bot=60, left=70, top=40, right=30 } by default
    fft_plot.setLogScale("y");

    fft_plot.setYLim(0.1f, yLim);
    int _nTicks = PApplet.parseInt(yLim/10 - 1); //number of axis subdivisions
    fft_plot.getYAxis().setNTicks(_nTicks);  //sets the number of axis divisions...
    fft_plot.setXLim(0.1f, xLim);
    fft_plot.getYAxis().setDrawTickLabels(true);
    fft_plot.setPointSize(2);
    fft_plot.setPointColor(0);

    //setup points of fft point arrays
    for (int i = 0; i < fft_points.length; i++) {
      fft_points[i] = new GPointsArray(FFT_indexLim);
    }

    //fill fft point arrays
    for (int i = 0; i < fft_points.length; i++) { //loop through each channel
      for (int j = 0; j < FFT_indexLim; j++) {
        //GPoint temp = new GPoint(i, 15*noise(0.1*i));
        //println(i + " " + j);
        GPoint temp = new GPoint(j, 0);
        fft_points[i].set(j, temp);
      }
    }

    //map fft point arrays to fft plots
    fft_plot.setPoints(fft_points[0]);
  }

  public void update(){

    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    //update the points of the FFT channel arrays
    //update fft point arrays
    // println("LENGTH = " + fft_points.length);
    // println("LENGTH = " + fftBuff.length);
    // println("LENGTH = " + FFT_indexLim);
    for (int i = 0; i < fft_points.length; i++) {
      for (int j = 0; j < FFT_indexLim + 2; j++) {  //loop through frequency domain data, and store into points array
        //GPoint powerAtBin = new GPoint(j, 15*random(0.1*j));
        GPoint powerAtBin;

        // println("i = " + i);
        // float a = get_fs_Hz_safe();
        // float aa = fftBuff[i].getBand(j);
        // float b = fftBuff[i].getBand(j);
        // float c = Nfft;

        powerAtBin = new GPoint((1.0f*get_fs_Hz_safe()/Nfft)*j, fftBuff[i].getBand(j));
        fft_points[i].set(j, powerAtBin);
        // GPoint powerAtBin = new GPoint((1.0*get_fs_Hz_safe()/Nfft)*j, fftBuff[i].getBand(j));

        //println("=========================================");
        //println(j);
        //println(fftBuff[i].getBand(j) + " :: " + fft_points[i].getX(j) + " :: " + fft_points[i].getY(j));
        //println("=========================================");
      }
    }

    //remap fft point arrays to fft plots
    fft_plot.setPoints(fft_points[0]);

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

    //draw FFT Graph w/ all plots
    noStroke();
    fft_plot.beginDraw();
    fft_plot.drawBackground();
    fft_plot.drawBox();
    fft_plot.drawXAxis();
    fft_plot.drawYAxis();
    //fft_plot.drawTopAxis();
    //fft_plot.drawRightAxis();
    //fft_plot.drawTitle();
    fft_plot.drawGridLines(2);
    //here is where we will update points & loop...
    for (int i = 0; i < fft_points.length; i++) {
      fft_plot.setLineColor(lineColor[i]);
      fft_plot.setPoints(fft_points[i]);
      fft_plot.drawLines();
      // fft_plot.drawPoints(); //draw points
    }
    fft_plot.endDraw();

    //for this widget need to redraw the grey bar, bc the FFT plot covers it up...
    fill(200, 200, 200);
    rect(x, y - navHeight, w, navHeight); //button bar

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    //update position/size of FFT plot
    fft_plot.setPos(x, y-navHeight);//update position
    fft_plot.setOuterDim(w, h+navHeight);//update dimensions

  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...

  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
//triggered when there is an event in the MaxFreq. Dropdown
public void MaxFreq(int n) {
  /* request the selected item based on index n */
  w_fft.fft_plot.setXLim(0.1f, w_fft.xLimOptions[n]); //update the xLim of the FFT_Plot
  closeAllDropdowns();
}

//triggered when there is an event in the VertScale Dropdown
public void VertScale(int n) {

  w_fft.fft_plot.setYLim(0.1f, w_fft.yLimOptions[n]); //update the yLim of the FFT_Plot
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void LogLin(int n) {
  if (n==0) {
    w_fft.fft_plot.setLogScale("y");
  } else {
    w_fft.fft_plot.setLogScale("");
  }
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void Smoothing(int n) {
  smoothFac_ind = n;
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void UnfiltFilt(int n) {
  if (n==0) {
    //have FFT use filtered data -- default
    isFFTFiltered = true;
  } else {
    //have FFT use unfiltered data
    isFFTFiltered = false;
  }
  closeAllDropdowns();
}

////////////////////////////////////////////////////
//
//    W_template.pde (ie "Widget Template")
//
//    This is a Template Widget, intended to be used as a starting point for OpenBCI Community members that want to develop their own custom widgets!
//    Good luck! If you embark on this journey, please let us know. Your contributions are valuable to everyone!
//
//    Created by: Conor Russomanno, November 2016
//
///////////////////////////////////////////////////,

class W_openBionics extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  PApplet parent;

  Serial OpenBionicsHand;
  PFont f = createFont("Arial Bold", 24); //for "FFT Plot" Widget Title
  PFont f2 = createFont("Arial", 18); //for dropdown name titles (above dropdown widgets)

  int parentContainer = 9; //which container is it mapped to by default?
  boolean thumbPressed,indexPressed,middlePressed,ringPressed,littlePressed,palmPressed = false;
  boolean researchMode = false;


  PImage hand;
  PImage thumb;
  PImage index;
  PImage middle;
  PImage ring;
  PImage little;
  PImage palm;
  int last_command;

  Button configClose;
  Button configConfirm;
  Button connect;
  MenuList obChanList;

  ControlP5 configP5;
  String obName;
  String obBaud;
  List serialListOB;
  List baudListOB;
  int drawConfig;
  int[] fingerChans;

  boolean wasConnected;


  W_openBionics(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function

    configP5 = new ControlP5(_parent);
    wasConnected = false;


    parent = _parent;
    baudListOB = Arrays.asList("NONE","230400","115200","57600","38400","28800","19200","14400","9600","7200","4800","3600","2400","1800","1200","600","300");
    drawConfig = -1;
    fingerChans = new int[6];
    for(int i = 0; i<6; i++) fingerChans[i] = -1;

    hand = loadImage("hand.png");
    thumb = loadImage("thumb_over.png");
    index = loadImage("index_over.png");
    middle = loadImage("middle_over.png");
    ring = loadImage("ring_over.png");
    little = loadImage("little_over.png");
    palm = loadImage("palm_over.png");

    String[] serialPortsLocal = Serial.list();
    serialListOB = new ArrayList();
    serialListOB.add("NONE");
    for (int i = 0; i < serialPortsLocal.length; i++) {
      String tempPort = serialPortsLocal[(serialPortsLocal.length-1) - i]; //list backwards... because usually our port is at the bottom
      if(!tempPort.equals(openBCI_portName)) serialListOB.add(tempPort);
    }

    configClose = new Button(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight,PApplet.parseInt(w/25.3f),PApplet.parseInt(w/25.3f),"X",fontInfo.buttonLabel_size);
    configConfirm = new Button(PApplet.parseInt(x) + w/2 + w/7,PApplet.parseInt(y) + 12*navHeight,PApplet.parseInt(w/10.12f),PApplet.parseInt(w/25.3f),"OKAY",fontInfo.buttonLabel_size);
    connect = new Button(PApplet.parseInt(x) + w - (w/7), PApplet.parseInt(y) + 10*navHeight, PApplet.parseInt(w/8), PApplet.parseInt(w/25.3f), "CONNECT", fontInfo.buttonLabel_size);

    obChanList = new MenuList(configP5, "obChanList", 100, 120, f2);
    obChanList.setPosition(x+w/3 + w/12, y + h/3 + h/16);
    obChanList.addItem(makeItem("NONE"));
    obChanList.activeItem = 0;
    for(int i = 0; i < nchan; i++) obChanList.addItem(makeItem("" + (i+1)));

    addDropdown("OpenBionicsSerialOut", "Serial Output", serialListOB, 0);
    addDropdown("BaudList", "Baud List", baudListOB, 0);
    configP5.get(MenuList.class, "obChanList").setVisible(false);
    // addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

  }
  public void process(){
    int output_normalized;
    StringBuilder researchCommand = new StringBuilder();

    if(OpenBionicsHand != null ){
        if(!researchMode){
          OpenBionicsHand.write("A10\n");
          researchMode = true;
        }
        byte inByte = PApplet.parseByte(OpenBionicsHand.read());

        println(inByte);
    }

    if(fingerChans[5] == -1){

        if(OpenBionicsHand != null){


        for(int i = 0; i<5; i++){
          //================= OpenBionics Analog Movement =======================
          if(fingerChans[i] == -1) output_normalized = 0;
          else output_normalized = PApplet.parseInt(map(w_emg.motorWidgets[fingerChans[i]].output_normalized, 0, 1, 0, 1023));

          if(i == 4) researchCommand.append(output_normalized + "\n");
          else researchCommand.append(output_normalized + ",");

        }
        OpenBionicsHand.write(researchCommand.toString());
      }
    }
    else {

      if(OpenBionicsHand != null){

        output_normalized = PApplet.parseInt(map(w_emg.motorWidgets[fingerChans[5]].output_normalized, 0, 1, 0, 100));
        OpenBionicsHand.write("G0P" + output_normalized + "\n");

      }

    }
  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    process();

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

    //configP5.setVisible(true);

    //draw FFT Graph w/ all plots
    noStroke();
    fill(255);
    rect(x, y, w, h);

    obChanList.setPosition(x+w/3 + w/12, y + h/3 + h/16);

    switch(drawConfig){
      case -1:
        image(hand,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );

        if(overThumb()) image(thumb,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        else if(overIndex()) image(index,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        else if(overMiddle()) image(middle,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        else if(overRing()) image(ring,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        else if(overLittle()) image(little,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        else if(overPalm()) image(palm,x + w/4,y+2*navHeight + 2, w/2,h/2 + h/3 );
        configP5.get(MenuList.class, "obChanList").setVisible(false);
        configP5.get(MenuList.class, "obChanList").activeItem = 0;
        if(wasConnected){
          fill(0,250,0);
          ellipse(x + 5 * (w/6) ,y + 7 * (h/10),20,20);
        }
        else{
          fill(250,0,0);
          ellipse(x + 5 * (w/6),y + 7 * (h/10),20,20);
        }
        connect.draw();
        break;
      case 0:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Thumb Channel Selection", x + w/3, y + 4*navHeight);
        break;
      case 1:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Index Finger Channel Selection", x + w/3, y + 4*navHeight);
        break;
      case 2:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Middle Finger Channel Selection", x + w/3, y + 4*navHeight);
        break;
      case 3:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Ring Finger Channel Selection", x + w/3, y + 4*navHeight);
        break;
      case 4:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Little Finger Channel Selection", x + w/3, y + 4*navHeight);
        break;
      case 5:
        configP5.get(MenuList.class, "obChanList").activeItem = fingerChans[drawConfig] + 1;
        configP5.get(MenuList.class, "obChanList").setVisible(true);
        fill(180,180,180);
        rect(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight, w/2, h/2 + 2*navHeight + navHeight/2);
        configClose.draw();
        configConfirm.draw();
        fill(10,10,10);
        textFont(f);
        textSize(12);
        text("Hand Channel Selection", x + w/3, y + 4*navHeight);
        break;
    }
    configP5.draw();

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    configClose = new Button(PApplet.parseInt(x) + w/4,PApplet.parseInt(y) + 3*navHeight,PApplet.parseInt(w/25.3f),PApplet.parseInt(w/25.3f),"X",fontInfo.buttonLabel_size);
    configConfirm = new Button(PApplet.parseInt(x) + w/2 + w/7,PApplet.parseInt(y) + 12*navHeight,PApplet.parseInt(w/10.12f),PApplet.parseInt(w/25.3f),"OKAY",fontInfo.buttonLabel_size);

    //update dropdown menu positions
    configP5.setGraphics(parent, 0, 0); //remaps the cp5 controller to the new PApplet window size
    int dropdownPos;
    int dropdownWidth = 60;
    dropdownPos = 1; //work down from 4 since we're starting on the right side now...
    configP5.getController("OpenBionicsSerialOut")
      .setPosition(x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1)), navHeight+(y+2)) //float right
      ;
    dropdownPos = 0;
    try{
    configP5.getController("LogLin")
      .setPosition(x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1)), navHeight+(y+2)) //float right
      ;
    }
    catch(Exception e){
      println("error resizing...");
    }

  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(drawConfig == -1){
      if(overThumb()) thumbPressed = true;
      else if(overIndex()) indexPressed = true;
      else if(overMiddle()) middlePressed = true;
      else if(overRing()) ringPressed = true;
      else if(overLittle()) littlePressed = true;
      else if(overPalm()) palmPressed = true;
      else if(connect.isMouseHere()) connect.wasPressed = true;
    }
    else{
      if(configClose.isMouseHere()) configClose.wasPressed= true;
      else if(configConfirm.isMouseHere()) configConfirm.wasPressed= true;
    }


  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
    if(drawConfig == -1){
      if (overThumb() && thumbPressed){drawConfig = 0;}
      else if (overIndex() && indexPressed){drawConfig= 1;}
      else if (overMiddle() && middlePressed){drawConfig = 2;}
      else if (overRing() && ringPressed){drawConfig = 3;}
      else if (overLittle() && littlePressed){drawConfig = 4;}
      else if (overPalm() && palmPressed){drawConfig = 5;}
      else if(connect.isMouseHere() && connect.wasPressed){

        //Connect to OpenBionics Hand
        try{

          OpenBionicsHand = new Serial(parent,obName,Integer.parseInt(obBaud));
          verbosePrint("Connected to OpenBionics Hand");
          wasConnected = true;
        }
        catch(Exception e){
          wasConnected = false;
          println(e);
          verbosePrint("Could not connect to OpenBionics Hand");
        }
      }

      thumbPressed = false;
      indexPressed = false;
      middlePressed = false;
      ringPressed = false;
      littlePressed = false;
      palmPressed = false;
      cursor(ARROW);


    }
    else{
      if(configClose.isMouseHere() && configClose.wasPressed) {
        configClose.wasPressed= false;
        drawConfig = -1;
      }
      else if(configConfirm.isMouseHere() && configConfirm.wasPressed){
        configConfirm.wasPressed= false;
        drawConfig = -1;
      }
    }

  }

  public boolean overThumb(){
    if(mouseX >= x + w/3.9f && mouseX <=x + w/2.5f && mouseY >= y + h/1.8f && mouseY <= y + h/1.32f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }
  public boolean overIndex(){
    if(mouseX >= x + w/2.65f && mouseX <=x + w/2.07f && mouseY >= y + h/4.89f && mouseY <= y + h/1.99f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }
  public boolean overMiddle(){
    if(mouseX >= x + w/2.01f && mouseX <=x + w/1.79f && mouseY >= y + h/7.08f && mouseY <= y + h/2.14f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }
  public boolean overRing(){
    if(mouseX >= x + w/1.73f && mouseX <=x + w/1.5f && mouseY >= y + h/5.59f && mouseY <= y + h/1.95f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }
  public boolean overLittle(){
    if(mouseX >= x + w/1.54f && mouseX <=x + w/1.34f && mouseY >= y + h/3.13f && mouseY <= y + h/1.78f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }
  public boolean overPalm(){
    if(mouseX >= x + w/2.47f && mouseX <=x + w/1.48f && mouseY >= y + h/1.89f && mouseY <= y + h/1.05f){
      cursor(HAND);
      return true;
    }
    else{
      cursor(ARROW);
      return false;
    }
  }

  //add custom classes functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget
  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void OpenBionicsSerialOut(int n){

  if(!w_openbionics.serialListOB.get(n).equals("NONE")) w_openbionics.obName = (String)w_openbionics.serialListOB.get(n);

  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

public void BaudList(int n){
  if(!w_openbionics.baudListOB.get(n).equals("NONE")) w_openbionics.obBaud = (String)w_openbionics.baudListOB.get(n);
  closeAllDropdowns();
}

public void obChanList(int n){
  w_openbionics.fingerChans[w_openbionics.drawConfig] = n - 1;
  closeAllDropdowns();
}

////////////////////////////////////////////////////
//
//    W_template.pde (ie "Widget Template")
//
//    This is a Template Widget, intended to be used as a starting point for OpenBCI Community members that want to develop their own custom widgets!
//    Good luck! If you embark on this journey, please let us know. Your contributions are valuable to everyone!
//
//    Created by: Conor Russomanno, November 2016
//
///////////////////////////////////////////////////,

class W_template extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  Button widgetTemplateButton;

  W_template(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    addDropdown("Dropdown1", "Drop 1", Arrays.asList("A", "B"), 0);
    addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

    widgetTemplateButton = new Button (x + w/2, y + h/2, 200, navHeight, "Design Your Own Widget!", 12);
    widgetTemplateButton.setFont(p4, 14);
    widgetTemplateButton.setURL("http://docs.openbci.com/OpenBCI%20Software/");

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();
    image(darwinLogo, x , y , w, h);
    fill(0,0,0);
    
    textFont(f5);
    textSize(h/18);
    text("Helping people reach out from the inside", x+ w/5, y+ h-30);  // Default depth, no z-value specifi
    //widgetTemplateButton.draw();

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    widgetTemplateButton.setPos(x + w/2 - widgetTemplateButton.but_dx/2, y + h/2 - widgetTemplateButton.but_dy/2);


  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(widgetTemplateButton.isMouseHere()){
      widgetTemplateButton.setIsActive(true);
    }

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(widgetTemplateButton.isActive && widgetTemplateButton.isMouseHere()){
      widgetTemplateButton.goToURL();
    }
    widgetTemplateButton.setIsActive(false);

  }

  //add custom functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget

  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void Dropdown1(int n){
  println("Item " + (n+1) + " selected from Dropdown 1");
  if(n==0){
    //do this
  } else if(n==1){
    //do this instead
  }

  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

public void Dropdown2(int n){
  println("Item " + (n+1) + " selected from Dropdown 2");
  closeAllDropdowns();
}

public void Dropdown3(int n){
  println("Item " + (n+1) + " selected from Dropdown 3");
  closeAllDropdowns();
}
////////////////////////////////////////////////////
//
// This class creates an Time Sereis Plot separate from the old Gui_Manager
// It extends the Widget class
//
// Conor Russomanno, November 2016
//
// Requires the plotting library from grafica ... replacing the old gwoptics (which is now no longer supported)
//
///////////////////////////////////////////////////


class W_timeSeries extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  int numChannelBars;
  float xF, yF, wF, hF;
  float ts_padding;
  float ts_x, ts_y, ts_h, ts_w; //values for actual time series chart (rectangle encompassing all channelBars)
  float plotBottomWell;
  float playbackWidgetHeight;
  int channelBarHeight;
  boolean showHardwareSettings = false;

  Button hardwareSettingsButton;

  ChannelBar[] channelBars;

  int[] xLimOptions = {3, 5, 8}; // number of seconds (x axis of graph)
  int[] yLimOptions = {0, 50, 100, 200, 400, 1000, 10000}; // 0 = Autoscale ... everything else is uV

  int xLim = xLimOptions[1];  //start at 5s
  int xMax = xLimOptions[0];  //start w/ autoscale

  boolean allowSpillover = false;

  HardwareSettingsController hsc;


  TextBox[] chanValuesMontage;
  TextBox[] impValuesMontage;
  boolean showMontageValues;

  private boolean visible = true;
  private boolean updating = true;

  int startingVertScaleIndex = 3;

  private boolean hasScrollbar = false;

  W_timeSeries(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function


    addDropdown("VertScale_TS", "Vert Scale", Arrays.asList("Auto", "50 uV", "100 uV", "200 uV", "400 uV", "1000 uV", "10000 uV"), startingVertScaleIndex);
    addDropdown("Duration", "Window", Arrays.asList("1 sec", "3 sec", "5 sec", "7 sec"), 2);
    // addDropdown("Spillover", "Spillover", Arrays.asList("False", "True"), 0);

    numChannelBars = nchan; //set number of channel bars = to current nchan of system (4, 8, or 16)

    xF = PApplet.parseFloat(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
    yF = PApplet.parseFloat(y);
    wF = PApplet.parseFloat(w);
    hF = PApplet.parseFloat(h);

    if(eegDataSource == DATASOURCE_PLAYBACKFILE && hasScrollbar){ //you will only ever see the playback widget in Playback Mode ... otherwise not visible
      playbackWidgetHeight = 50.0f;
    } else{
      playbackWidgetHeight = 0.0f;
    }

    plotBottomWell = 45.0f; //this appears to be an arbitrary vertical space adds GPlot leaves at bottom, I derived it through trial and error
    ts_padding = 10.0f;
    ts_x = xF + ts_padding;
    ts_y = yF + (ts_padding);
    ts_w = wF - ts_padding*2;
    ts_h = hF - playbackWidgetHeight - plotBottomWell - (ts_padding*2);
    channelBarHeight = PApplet.parseInt(ts_h/numChannelBars);

    channelBars = new ChannelBar[numChannelBars];

    //create our channel bars and populate our channelBars array!
    for(int i = 0; i < numChannelBars; i++){
      int channelBarY = PApplet.parseInt(ts_y) + i*(channelBarHeight); //iterate through bar locations
      ChannelBar tempBar = new ChannelBar(_parent, i+1, PApplet.parseInt(ts_x), channelBarY, PApplet.parseInt(ts_w), channelBarHeight); //int _channelNumber, int _x, int _y, int _w, int _h
      channelBars[i] = tempBar;
    }

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      hardwareSettingsButton = new Button((int)(x + 3), (int)(y + navHeight + 3), 120, navHeight - 6, "Hardware Settings", 12);
      hardwareSettingsButton.setCornerRoundess((int)(navHeight-6));
      hardwareSettingsButton.setFont(p6,10);
      // hardwareSettingsButton.setStrokeColor((int)(color(150)));
      // hardwareSettingsButton.setColorNotPressed(openbciBlue);
      hardwareSettingsButton.setColorNotPressed(color(57,128,204));
      hardwareSettingsButton.textColorNotActive = color(255);
      // hardwareSettingsButton.setStrokeColor((int)(color(138, 182, 229, 100)));
      hardwareSettingsButton.hasStroke(false);
      // hardwareSettingsButton.setColorNotPressed((int)(color(138, 182, 229)));
      hardwareSettingsButton.setHelpText("The buttons in this panel allow you to adjust the hardware settings of the OpenBCI Board.");
    }

    int x_hsc = PApplet.parseInt(ts_x);
    int y_hsc = PApplet.parseInt(ts_y);
    int w_hsc = PApplet.parseInt(ts_w); //width of montage controls (on left of montage)
    int h_hsc = PApplet.parseInt(ts_h); //height of montage controls (on left of montage)

    hsc = new HardwareSettingsController((int)channelBars[0].plot.getPos()[0] + 2, (int)channelBars[0].plot.getPos()[1], (int)channelBars[0].plot.getOuterDim()[0], h_hsc - 4, channelBarHeight);
  }

  public boolean isVisible() {
    return visible;
  }
  public boolean isUpdating() {
    return updating;
  }

  public void setVisible(boolean _visible) {
    visible = _visible;
  }
  public void setUpdating(boolean _updating) {
    updating = _updating;
  }

  public void update(){
    if(visible && updating){
      super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

      //put your code here...
      hsc.update(); //update channel controller

      //update channel bars ... this means feeding new EEG data into plots
      for(int i = 0; i < numChannelBars; i++){
        channelBars[i].update();
      }
    }
  }

  public void draw(){
    if(visible){
      super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

      //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class

      pushStyle();
      //draw channel bars
      for(int i = 0; i < numChannelBars; i++){
        channelBars[i].draw();
      }

      if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
        hardwareSettingsButton.draw();
      }

      //temporary placeholder for playback controller widget
      if(eegDataSource == DATASOURCE_PLAYBACKFILE && hasScrollbar){ //you will only ever see the playback widget in Playback Mode ... otherwise not visible
        pushStyle();
        fill(0,0,0,20);
        stroke(31,69,110);
        rect(xF, ts_y + ts_h + playbackWidgetHeight + 5, wF, playbackWidgetHeight);
        popStyle();
      } else{
        //dont draw anything at the bottom
      }

      //draw channel controller
      hsc.draw();

      popStyle();
    }
  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    xF = PApplet.parseFloat(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
    yF = PApplet.parseFloat(y);
    wF = PApplet.parseFloat(w);
    hF = PApplet.parseFloat(h);

    ts_x = xF + ts_padding;
    ts_y = yF + (ts_padding);
    ts_w = wF - ts_padding*2;
    ts_h = hF - playbackWidgetHeight - plotBottomWell - (ts_padding*2);
    channelBarHeight = PApplet.parseInt(ts_h/numChannelBars);

    for(int i = 0; i < numChannelBars; i++){
      int channelBarY = PApplet.parseInt(ts_y) + i*(channelBarHeight); //iterate through bar locations
      channelBars[i].screenResized(PApplet.parseInt(ts_x), channelBarY, PApplet.parseInt(ts_w), channelBarHeight); //bar x, bar y, bar w, bar h
    }

    hsc.screenResized((int)channelBars[0].plot.getPos()[0] + 2, (int)channelBars[0].plot.getPos()[1], (int)channelBars[0].plot.getOuterDim()[0], (int)ts_h - 4, channelBarHeight);

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      hardwareSettingsButton.setPos((int)(x0 + 3), (int)(y0 + navHeight + 3));
    }
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)


    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      //put your code here...
      if (hardwareSettingsButton.isMouseHere()) {
        hardwareSettingsButton.setIsActive(true);
      }
    }

    if(hsc.isVisible){
      hsc.mousePressed();
    } else {
      for(int i = 0; i < channelBars.length; i++){
        channelBars[i].mousePressed();
      }
    }


  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      //put your code here...
      if(hardwareSettingsButton.isActive && hardwareSettingsButton.isMouseHere()){
        println("toggle...");
        if(showHardwareSettings){
          showHardwareSettings = false;
          hsc.isVisible = false;
          hardwareSettingsButton.setString("Hardware Settings");
        } else{
          showHardwareSettings = true;
          hsc.isVisible = true;
          hardwareSettingsButton.setString("Time Series");
        }
      }
      hardwareSettingsButton.setIsActive(false);
    }

    if(hsc.isVisible){
      hsc.mouseReleased();
    } else {
      for(int i = 0; i < channelBars.length; i++){
        channelBars[i].mouseReleased();
      }
    }
  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void VertScale_TS(int n) {
  if (n==0) { //autoscale
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(0);
    }
  } else if(n==1) { //50uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(50);
    }
  } else if(n==2) { //100uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(100);
    }
  } else if(n==3) { //200uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(200);
    }
  } else if(n==4) { //400uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(400);
    }
  } else if(n==5) { //1000uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(1000);
    }
  } else if(n==6) { //10000uV
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustVertScale(10000);
    }
  }
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void Duration(int n) {
  // println("adjust duration to: ");
  if(n==0){ //set time series x axis to 1 secconds
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustTimeAxis(1);
    }
  } else if(n==1){ //set time series x axis to 3 secconds
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustTimeAxis(3);
    }
  } else if(n==2){ //set to 5 seconds
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustTimeAxis(5);
    }
  } else if(n==3){ //set to 7 seconds (max due to arry size ... 2000 total packets saved)
    for(int i = 0; i < w_timeSeries.numChannelBars; i++){
      w_timeSeries.channelBars[i].adjustTimeAxis(7);
    }
  }
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void Spillover(int n) {
  if (n==0) {
    w_timeSeries.allowSpillover = false;
  } else {
    w_timeSeries.allowSpillover = true;
  }
  closeAllDropdowns();
}













//========================================================================================================================
//                      CHANNEL BAR CLASS -- Implemented by Time Series Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
class ChannelBar{

  int channelNumber; //duh
  String channelString;
  int x, y, w, h;
  boolean isOn; //true means data is streaming and channel is active on hardware ... this will send message to OpenBCI Hardware
  Button onOffButton;
  int onOff_diameter, impButton_diameter;
  Button impCheckButton;

  GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
  GPointsArray channelPoints;
  int nPoints;
  int numSeconds;
  float timeBetweenPoints;

  int channelColor; //color of plot trace

  boolean isAutoscale; //when isAutoscale equals true, the y-axis of each channelBar will automatically update to scale to the largest visible amplitude
  int autoScaleYLim = 0;

  TextBox voltageValue;
  TextBox impValue;

  boolean drawVoltageValue;
  boolean drawImpValue;

  ChannelBar(PApplet _parent, int _channelNumber, int _x, int _y, int _w, int _h){ // channel number, x/y location, height, width

    channelNumber = _channelNumber;
    channelString = str(channelNumber);
    isOn = true;

    x = _x;
    y = _y;
    w = _w;
    h = _h;

    if(h > 26){
      onOff_diameter = 26;
    } else{
      onOff_diameter = h - 2;
    }

    onOffButton = new Button (x + 6, y + PApplet.parseInt(h/2) - PApplet.parseInt(onOff_diameter/2), onOff_diameter, onOff_diameter, channelString, fontInfo.buttonLabel_size);
    onOffButton.setFont(h2, 16);
    onOffButton.setCircleButton(true);
    onOffButton.setColorNotPressed(channelColors[(channelNumber-1)%8]);
    onOffButton.hasStroke(false);

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      impButton_diameter = 22;
      impCheckButton = new Button (x + 36, y + PApplet.parseInt(h/2) - PApplet.parseInt(impButton_diameter/2), impButton_diameter, impButton_diameter, "\u2126", fontInfo.buttonLabel_size);
      impCheckButton.setFont(h2, 16);
      impCheckButton.setCircleButton(true);
      impCheckButton.setColorNotPressed(color(255));
      impCheckButton.hasStroke(false);
    } else {
      impButton_diameter = 0;
    }

    numSeconds = 5;
    plot = new GPlot(_parent);
    plot.setPos(x + 36 + 4 + impButton_diameter, y);
    plot.setDim(w - 36 - 4 - impButton_diameter, h);
    plot.setMar(0f, 0f, 0f, 0f);
    plot.setLineColor((int)channelColors[(channelNumber-1)%8]);
    plot.setXLim(-5,0);
    plot.setYLim(-200,200);
    plot.setPointSize(2);
    plot.setPointColor(0);

    if(channelNumber == nchan){
      plot.getXAxis().setAxisLabelText("Time (s)");
    }
    // plot.setBgColor(color(31,69,110));

    nPoints = nPointsBasedOnDataSource();

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      nPoints = numSeconds * (int)openBCI.fs_Hz;
    }else if(eegDataSource == DATASOURCE_GANGLION || nchan == 4){
      nPoints = numSeconds * (int)ganglion.fs_Hz;
    }else{
      nPoints = numSeconds * (int)openBCI.fs_Hz;
    }

    channelPoints = new GPointsArray(nPoints);
    timeBetweenPoints = (float)numSeconds / (float)nPoints;

    for (int i = 0; i < nPoints; i++) {
      float time = -(float)numSeconds + (float)i*timeBetweenPoints;
      // float time = (-float(numSeconds))*(float(i)/float(nPoints));
      // float filt_uV_value = dataBuffY_filtY_uV[channelNumber-1][dataBuffY_filtY_uV.length-nPoints];
      float filt_uV_value = 0.0f; //0.0 for all points to start
      GPoint tempPoint = new GPoint(time, filt_uV_value);
      channelPoints.set(i, tempPoint);
    }

    plot.setPoints(channelPoints); //set the plot with 0.0 for all channelPoints to start

    voltageValue = new TextBox("", x + 36 + 4 + impButton_diameter + (w - 36 - 4 - impButton_diameter) - 2, y + h);
    voltageValue.textColor = color(bgColor);
    voltageValue.alignH = RIGHT;
    // voltageValue.alignV = TOP;
    voltageValue.drawBackground = true;
    voltageValue.backgroundColor = color(255,255,255,125);

    impValue = new TextBox("", x + 36 + 4 + impButton_diameter + 2, y + h);
    impValue.textColor = color(bgColor);
    impValue.alignH = LEFT;
    // impValue.alignV = TOP;
    impValue.drawBackground = true;
    impValue.backgroundColor = color(255,255,255,125);

    drawVoltageValue = true;
    drawImpValue = false;

  }

  public void update(){

    //update the voltage value text string
    String fmt; float val;

    //update the voltage values
    val = dataProcessing.data_std_uV[channelNumber-1];
    voltageValue.string = String.format(getFmt(val),val) + " uVrms";
    if (is_railed != null) {
      if (is_railed[channelNumber-1].is_railed == true) {
        voltageValue.string = "RAILED";
      } else if (is_railed[channelNumber-1].is_railed_warn == true) {
        voltageValue.string = "NEAR RAILED - " + String.format(getFmt(val),val) + " uVrms";
      }
    }

    //update the impedance values
    val = data_elec_imp_ohm[channelNumber-1]/1000;
    impValue.string = String.format(getFmt(val),val) + " kOhm";
    if (is_railed != null) {
      if (is_railed[channelNumber-1].is_railed == true) {
        impValue.string = "RAILED";
      }
    }

    // update data in plot
    updatePlotPoints();
    if(isAutoscale){
      autoScale();
    }
  }

  private String getFmt(float val) {
    String fmt;
      if (val > 100.0f) {
        fmt = "%.0f";
      } else if (val > 10.0f) {
        fmt = "%.1f";
      } else {
        fmt = "%.2f";
      }
      return fmt;
  }

  public void updatePlotPoints(){
    // update data in plot
    if(dataBuffY_filtY_uV[channelNumber-1].length > nPoints){
      for (int i = dataBuffY_filtY_uV[channelNumber-1].length - nPoints; i < dataBuffY_filtY_uV[channelNumber-1].length; i++) {
        float time = -(float)numSeconds + (float)(i-(dataBuffY_filtY_uV[channelNumber-1].length-nPoints))*timeBetweenPoints;
        float filt_uV_value = dataBuffY_filtY_uV[channelNumber-1][i];
        // float filt_uV_value = 0.0;
        GPoint tempPoint = new GPoint(time, filt_uV_value);
        channelPoints.set(i-(dataBuffY_filtY_uV[channelNumber-1].length-nPoints), tempPoint);
      }
      plot.setPoints(channelPoints); //reset the plot with updated channelPoints
    }
  }

  public void draw(){
    pushStyle();

    //draw channel holder background
    stroke(31,69,110, 50);
    fill(255);
    rect(x,y,w,h);

    //draw onOff Button
    onOffButton.draw();
    //draw impedance check Button
    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      impCheckButton.draw();
    }

    //draw plot
    stroke(31,69,110, 50);
    fill(color(125,30,12,30));

    rect(x + 36 + 4 + impButton_diameter, y, w - 36 - 4 - impButton_diameter, h);

    plot.beginDraw();
    plot.drawBox(); // we won't draw this eventually ...
    plot.drawGridLines(0);
    plot.drawLines();
    // plot.drawPoints();
    // plot.drawYAxis();
    if(channelNumber == nchan){ //only draw the x axis label on the bottom channel bar
      plot.drawXAxis();
      plot.getXAxis().draw();
    }
    plot.endDraw();

    if(drawImpValue){
      impValue.draw();
    }
    if(drawVoltageValue){
      voltageValue.draw();
    }

    popStyle();
  }

  public void setDrawImp(boolean _trueFalse){
    drawImpValue = _trueFalse;
  }

  public int nPointsBasedOnDataSource(){
    int _nPoints;
    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      _nPoints = numSeconds * (int)openBCI.fs_Hz;
    }else if(eegDataSource == DATASOURCE_GANGLION || nchan == 4){
      _nPoints = numSeconds * (int)ganglion.fs_Hz;
    }else{
      _nPoints = numSeconds * (int)openBCI.fs_Hz;
    }

    return _nPoints;
  }

  public void adjustTimeAxis(int _newTimeSize){
    numSeconds = _newTimeSize;
    plot.setXLim(-_newTimeSize,0);

    nPoints = nPointsBasedOnDataSource();

    channelPoints = new GPointsArray(nPoints);
    if(_newTimeSize > 1){
      plot.getXAxis().setNTicks(_newTimeSize);  //sets the number of axis divisions...
    }else{
      plot.getXAxis().setNTicks(10);
    }
    if(w_timeSeries.isUpdating()){
      updatePlotPoints();
    }
    // println("New X axis = " + _newTimeSize);
  }

  public void adjustVertScale(int _vertScaleValue){
    if(_vertScaleValue == 0){
      isAutoscale = true;
    } else {
      isAutoscale = false;
      plot.setYLim(-_vertScaleValue, _vertScaleValue);
    }
  }

  public void autoScale(){
    autoScaleYLim = 0;
    for(int i = 0; i < nPoints; i++){
      if(PApplet.parseInt(abs(channelPoints.getY(i))) > autoScaleYLim){
        autoScaleYLim = PApplet.parseInt(abs(channelPoints.getY(i)));
      }
    }
    plot.setYLim(-autoScaleYLim, autoScaleYLim);
  }

  public void screenResized(int _x, int _y, int _w, int _h){
    x = _x;
    y = _y;
    w = _w;
    h = _h;

    if(h > 26){
      onOff_diameter = 26;
      onOffButton.but_dx = onOff_diameter;
      onOffButton.but_dy = onOff_diameter;
    } else{
      // println("h = " + h);
      onOff_diameter = h - 2;
      onOffButton.but_dx = onOff_diameter;
      onOffButton.but_dy = onOff_diameter;
    }

    onOffButton.but_x = x + 6;
    onOffButton.but_y = y + PApplet.parseInt(h/2) - PApplet.parseInt(onOff_diameter/2);

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      impCheckButton.but_x = x + 36;
      impCheckButton.but_y = y + PApplet.parseInt(h/2) - PApplet.parseInt(impButton_diameter/2);
    }

    //reposition & resize the plot
    plot.setPos(x + 36 + 4 + impButton_diameter, y);
    plot.setDim(w - 36 - 4 - impButton_diameter, h);

    voltageValue.x = x + 36 + 4 + impButton_diameter + (w - 36 - 4 - impButton_diameter) - 2;
    voltageValue.y = y + h;
    impValue.x = x + 36 + 4 + impButton_diameter + 2;
    impValue.y = y + h;

  }

  public void mousePressed(){
    if(onOffButton.isMouseHere()){
      println("[" + channelNumber + "] onOff pressed");
      onOffButton.setIsActive(true);
    }

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      if(impCheckButton.isMouseHere()){
        println("[" + channelNumber + "] imp pressed");
        impCheckButton.setIsActive(true);
      }
    }

  }

  public void mouseReleased(){
    if(onOffButton.isMouseHere()){
      println("[" + channelNumber + "] onOff released");
      if(isOn){  // if channel is active
        isOn = false; // deactivate it
        deactivateChannel(channelNumber - 1); //got to - 1 to make 0 indexed
        onOffButton.setColorNotPressed(color(50));
      }
      else { // if channel is not active
        isOn = true;
        activateChannel(channelNumber - 1);       // activate it
        onOffButton.setColorNotPressed(channelColors[(channelNumber-1)%8]);
      }
    }

    onOffButton.setIsActive(false);

    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      if(impCheckButton.isMouseHere() && impCheckButton.isActive()){
        println("[" + channelNumber + "] imp released");
        w_timeSeries.hsc.toggleImpedanceCheck(channelNumber-1);  // 'n' indicates the N inputs and '1' indicates test impedance
        if(drawImpValue){
          drawImpValue = false;
          impCheckButton.setColorNotPressed(color(255));
        } else {
          drawImpValue = true;
          impCheckButton.setColorNotPressed(color(50));
        }
      }
      impCheckButton.setIsActive(false);
    }
  }
};

//========================================================================================================================
//                                          END OF -- CHANNEL BAR CLASS
//========================================================================================================================

//============= PLAYBACKSLIDER =============
class PlaybackScrollbar {
  int swidth, sheight;    // width and height of bar
  float xpos, ypos;       // x and y position of bar
  float spos, newspos;    // x position of slider
  float sposMin, sposMax; // max and min values of slider
  boolean over;           // is the mouse over the slider?
  boolean locked;
  float ratio;
  int num_indices;

  PlaybackScrollbar (float xp, float yp, int sw, int sh, int is) {
    swidth = sw;
    sheight = sh;
    int widthtoheight = sw - sh;
    ratio = (float)sw / (float)widthtoheight;
    xpos = xp;
    ypos = yp-sheight/2;
    spos = xpos;
    newspos = spos;
    sposMin = xpos;
    sposMax = xpos + swidth - sheight/2;
    num_indices = is;
  }

  public void update() {
    if (overEvent()) {
      over = true;
    } else {
      over = false;
    }
    if (mousePressed && over) {
      locked = true;
    }
    if (!mousePressed) {
      locked = false;
    }
    if (locked) {
      newspos = constrain(mouseX-sheight/2, sposMin, sposMax);
    }
    if (abs(newspos - spos) > 1) {
      spos = spos + (newspos-spos);
    }
  }

  public float constrain(float val, float minv, float maxv) {
    return min(max(val, minv), maxv);
  }

  public boolean overEvent() {
    if (mouseX > xpos && mouseX < xpos+swidth &&
       mouseY > ypos && mouseY < ypos+sheight) {
      cursor(HAND);
      return true;
    } else {
      cursor(ARROW);
      return false;
    }
  }

  public int get_index(){

    float seperate_val = sposMax / num_indices;

    int index;

    for(index = 0; index < num_indices + 1; index++){
      if(getPos() >= seperate_val * index && getPos() <= seperate_val * (index +1) ) return index;
      else if(index == num_indices && getPos() >= seperate_val * index) return num_indices;
    }

    return -1;
  }

  public void display() {
    noStroke();
    fill(204);
    rect(xpos, ypos, swidth, sheight);
    if (over || locked) {
      fill(0, 0, 0);
    } else {
      fill(102, 102, 102);
    }
    rect(spos, ypos, sheight/2, sheight);
  }

  public float getPos() {
    // Convert spos to be values between
    // 0 and the total width of the scrollbar
    return spos * ratio;
  }
};

//WORK WITH COLIN ON IMPLEMENTING THIS ABOVE
/*
if(has_processed){
  if(scrollbar == null) scrollbar = new PlaybackScrollbar(10,height/20 * 19, width/2 - 10, 16, indices);
  else {
    float val_uV = 0.0f;
    boolean foundIndex =true;
    int startIndex = 0;

    scrollbar.update();
    scrollbar.display();
    //println(index_of_times.get(scrollbar.get_index()));
    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
    ArrayList<Date> keys_to_plot = new ArrayList();

    try{
      Date timeIndex = format.parse(index_of_times.get(scrollbar.get_index()));
      Date fiveBefore = new Date(timeIndex.getTime());
      fiveBefore.setTime(fiveBefore.getTime() - 5000);
      Date fiveBeforeCopy = new Date(fiveBefore.getTime());

      //START HERE TOMORROW

      int i = 0;
      int timeToBreak = 0;
      while(true){
        //println("in while i:" + i);
        if(index_of_times.get(i).contains(format.format(fiveBeforeCopy).toString())){
          println("found");
          startIndex = i;
          break;
        }
        if(i == index_of_times.size() -1){
          i = 0;
          fiveBeforeCopy.setTime(fiveBefore.getTime() + 1);
          timeToBreak++;
        }
        if(timeToBreak > 3){
          break;
        }
        i++;

      }
      println("after first while");

      while(fiveBefore.before(timeIndex)){
       //println("in while :" + fiveBefore);
        if(index_of_times.get(startIndex).contains(format.format(fiveBefore).toString())){
          keys_to_plot.add(fiveBefore);
          startIndex++;
        }
        //println(fiveBefore);
        fiveBefore.setTime(fiveBefore.getTime() + 1);
      }
      println("keys_to_plot size: " + keys_to_plot.size());
    }
    catch(Exception e){}

    float[][] data = new float[keys_to_plot.size()][nchan];
    int i = 0;

    for(Date elm : keys_to_plot){

      for(int Ichan=0; Ichan < nchan; Ichan++){
        val_uV = processed_file.get(elm)[Ichan][startIndex];


        data[Ichan][i] = (int) (0.5f+ val_uV / openBCI.get_scale_fac_uVolts_per_count()); //convert to counts, the 0.5 is to ensure roundi
      }
      i++;
    }

    //println(keys_to_plot.size());
    if(keys_to_plot.size() > 100){
    for(int Ichan=0; Ichan<nchan; Ichan++){
      update(data[Ichan],data_elec_imp_ohm);
    }
    }
    //for(int index = 0; index <= scrollbar.get_index(); index++){
    //  //yLittleBuff_uV = processed_file.get(index_of_times.get(index));

    //}

    cc.update();
    cc.draw();
  }
}
*/

////////////////////////////////////////////////////
//
//  W_accelerometer is used to visiualze accelerometer data
//
//  Created: Joel Murphy
//  Modified: Colin Fausnaught, September 2016
//  Modified: Wangshu Sun, November 2016
//
//
///////////////////////////////////////////////////,

class W_accelerometer extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  // color boxBG;
  int graphStroke = 0xffd2d2d2;
  int graphBG = 0xfff5f5f5;
  int textColor = 0xff000000;

  int strokeColor;

  // Accelerometer Stuff
  int AccelBuffSize = 500; //points registered in accelerometer buff

  int padding = 30;

  // bottom xyz graph
  int AccelWindowWidth;
  int AccelWindowHeight;
  int AccelWindowX;
  int AccelWindowY;

  // circular 3d xyz graph
  float PolarWindowX;
  float PolarWindowY;
  int PolarWindowWidth;
  int PolarWindowHeight;
  float PolarCorner;

  int eggshell;
  int Xcolor;
  int Ycolor;
  int Zcolor;

  float yMaxMin;

  float currentXvalue;
  float currentYvalue;
  float currentZvalue;

  int[] X;
  int[] Y;
  int[] Z;

  float dummyX;
  float dummyY;
  float dummyZ;
  boolean Xrising;
  boolean Yrising;
  boolean Zrising;
  boolean OBCI_inited= true;

  Button accelModeButton;

  W_accelerometer(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    // boxBG = bgColor;
    strokeColor = color(138, 146, 153);

    // Accel Sensor Stuff
    eggshell = color(255, 253, 248);
    Xcolor = color(224, 56, 45);
    Ycolor = color(49, 113, 89);
    Zcolor = color(54, 87, 158);

    setGraphDimensions();

    yMaxMin = adjustYMaxMinBasedOnSource();

    // XYZ buffer for bottom graph
    X = new int[AccelBuffSize];
    Y = new int[AccelBuffSize];
    Z = new int[AccelBuffSize];

    // for synthesizing values
    Xrising = true;
    Yrising = false;
    Zrising = true;

    // initialize data
    for (int i=0; i<X.length; i++) {  // initialize the accelerometer data
      X[i] = AccelWindowY + AccelWindowHeight/4; // X at 1/4
      Y[i] = AccelWindowY + AccelWindowHeight/2;  // Y at 1/2
      Z[i] = AccelWindowY + (AccelWindowHeight/4)*3;  // Z at 3/4
    }

    if(eegDataSource == DATASOURCE_GANGLION){
      // accelModeButton = new Button((int)(x + w/2), (int)(y +80), 120, navHeight - 6, "Turn Accel. On", 12);
      accelModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Accel. On", 12);
      accelModeButton.setCornerRoundess((int)(navHeight-6));
      accelModeButton.setFont(p6,10);
      // accelModeButton.setStrokeColor((int)(color(150)));
      // accelModeButton.setColorNotPressed(openbciBlue);
      accelModeButton.setColorNotPressed(color(57,128,204));
      accelModeButton.textColorNotActive = color(255);
      // accelModeButton.setStrokeColor((int)(color(138, 182, 229, 100)));
      accelModeButton.hasStroke(false);
      // accelModeButton.setColorNotPressed((int)(color(138, 182, 229)));
      accelModeButton.setHelpText("Click this button to activate/deactivate the accelerometer of your Ganglion board!");
    }

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    // addDropdown("Thisdrop", "Drop 1", Arrays.asList("A", "B"), 0);
    // addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    // addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

  }

  public void initPlayground(OpenBCI_ADS1299 _OBCI) {
    OBCI_inited = true;
  }

  public float adjustYMaxMinBasedOnSource(){
    float _yMaxMin;
    if(eegDataSource == DATASOURCE_NORMAL_W_AUX){
      _yMaxMin = 4.0f;
    }else if(eegDataSource == DATASOURCE_GANGLION || nchan == 4){
      _yMaxMin = 2.0f;
    }else{
      _yMaxMin = 4.0f;
    }

    return _yMaxMin;
  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    if (isRunning) {
      if (eegDataSource == DATASOURCE_SYNTHETIC) {
        synthesizeAccelerometerData();
        currentXvalue = map(X[X.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight, yMaxMin, -yMaxMin);
        currentYvalue = map(Y[Y.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight, yMaxMin, -yMaxMin);
        currentZvalue = map(Z[Z.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight, yMaxMin, -yMaxMin);
        shiftWave();
      } else if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {
        currentXvalue = openBCI.validAuxValues[0] * openBCI.get_scale_fac_accel_G_per_count();
        currentYvalue = openBCI.validAuxValues[1] * openBCI.get_scale_fac_accel_G_per_count();
        currentZvalue = openBCI.validAuxValues[2] * openBCI.get_scale_fac_accel_G_per_count();
        X[X.length-1] =
          PApplet.parseInt(map(currentXvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        X[X.length-1] = constrain(X[X.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);
        Y[Y.length-1] =
          PApplet.parseInt(map(currentYvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        Y[Y.length-1] = constrain(Y[Y.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);
        Z[Z.length-1] =
          PApplet.parseInt(map(currentZvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        Z[Z.length-1] = constrain(Z[Z.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);

        shiftWave();
      } else if (eegDataSource == DATASOURCE_GANGLION) {
        currentXvalue = ganglion.accelArray[0] * ganglion.get_scale_fac_accel_G_per_count();
        currentYvalue = ganglion.accelArray[1] * ganglion.get_scale_fac_accel_G_per_count();
        currentZvalue = ganglion.accelArray[2] * ganglion.get_scale_fac_accel_G_per_count();
        X[X.length-1] =
          PApplet.parseInt(map(currentXvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        X[X.length-1] = constrain(X[X.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);
        Y[Y.length-1] =
          PApplet.parseInt(map(currentYvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        Y[Y.length-1] = constrain(Y[Y.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);
        Z[Z.length-1] =
          PApplet.parseInt(map(currentZvalue, -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));
        Z[Z.length-1] = constrain(Z[Z.length-1], AccelWindowY, AccelWindowY+AccelWindowHeight);

        shiftWave();
      } else {  // playback data
        currentXvalue = accelerometerBuff[0][accelerometerBuff[0].length-1];
        currentYvalue = accelerometerBuff[1][accelerometerBuff[1].length-1];
        currentZvalue = accelerometerBuff[2][accelerometerBuff[2].length-1];
      }
    }
  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    pushStyle();
    //put your code here...
    //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    if (true) {
      // fill(graphBG);
      // stroke(strokeColor);
      // rect(x, y, w, h);
      // textFont(f4, 24);
      // textAlign(LEFT, TOP);
      // fill(textColor);
      // text("Acellerometer Gs", x + 10, y + 10);

      fill(50);
      textFont(p4, 14);
      textAlign(CENTER,CENTER);
      text("z", PolarWindowX, (PolarWindowY-PolarWindowHeight/2)-12);
      text("x", (PolarWindowX+PolarWindowWidth/2)+8, PolarWindowY-5);
      text("y", (PolarWindowX+PolarCorner)+10, (PolarWindowY-PolarCorner)-10);

      fill(graphBG);  // pulse window background
      stroke(graphStroke);
      rect(AccelWindowX, AccelWindowY, AccelWindowWidth, AccelWindowHeight);
      line(AccelWindowX, AccelWindowY + AccelWindowHeight/2, AccelWindowX+AccelWindowWidth, AccelWindowY + AccelWindowHeight/2); //midline

      fill(50);
      textFont(p5, 12);
      textAlign(CENTER,CENTER);
      text("+"+(int)yMaxMin+"g", AccelWindowX+AccelWindowWidth + 12, AccelWindowY);
      text("0g", AccelWindowX+AccelWindowWidth + 12, AccelWindowY + AccelWindowHeight/2);
      text("-"+(int)yMaxMin+"g", AccelWindowX+AccelWindowWidth + 12, AccelWindowY + AccelWindowHeight);


      fill(graphBG);  // pulse window background
      stroke(graphStroke);
      ellipse(PolarWindowX,PolarWindowY,PolarWindowWidth,PolarWindowHeight);

      stroke(180);
      line(PolarWindowX-PolarWindowWidth/2, PolarWindowY, PolarWindowX+PolarWindowWidth/2, PolarWindowY);
      line(PolarWindowX, PolarWindowY-PolarWindowHeight/2, PolarWindowX, PolarWindowY+PolarWindowHeight/2);
      line(PolarWindowX-PolarCorner, PolarWindowY+PolarCorner, PolarWindowX+PolarCorner, PolarWindowY-PolarCorner);

      fill(50);
      textFont(p3, 16);

      if (eegDataSource == DATASOURCE_NORMAL_W_AUX) {  // LIVE
        // fill(Xcolor);
        // text("X " + nf(currentXvalue, 1, 3), x+10, y+40);
        // fill(Ycolor);
        // text("Y " + nf(currentYvalue, 1, 3), x+10, y+80);
        // fill(Zcolor);
        // text("Z " + nf(currentZvalue, 1, 3), x+10, y+120);
        drawAccValues();
        draw3DGraph();
        drawAccWave();
      } else if (eegDataSource == DATASOURCE_GANGLION) {
        accelModeButton.draw();
        drawAccValues();
        draw3DGraph();
        drawAccWave();
      } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  // SYNTHETIC
        // fill(Xcolor);
        // text("X "+nf(currentXvalue, 1, 3), x+10, y+40);
        // fill(Ycolor);
        // text("Y "+nf(currentYvalue, 1, 3), x+10, y+80);
        // fill(Zcolor);
        // text("Z "+nf(currentZvalue, 1, 3), x+10, y+120);
        drawAccValues();
        draw3DGraph();
        drawAccWave();
      }
      else {  // PLAYBACK
        drawAccValues();
        draw3DGraph();
        drawAccWave2();
      }
    }

    // pushStyle();
    // textFont(h1,24);
    // fill(bgColor);
    // textAlign(CENTER,CENTER);
    // text(widgetTitle, x + w/2, y + h/2);
    // popStyle();
    popStyle();
  }

  public void setGraphDimensions(){
    AccelWindowWidth = w - padding*2;
    AccelWindowHeight = PApplet.parseInt((PApplet.parseFloat(h) - PApplet.parseFloat(padding*3))/2.0f);
    AccelWindowX = x + padding;
    AccelWindowY = y + h - AccelWindowHeight - padding;

    // PolarWindowWidth = 155;
    // PolarWindowHeight = 155;
    PolarWindowWidth = AccelWindowHeight;
    PolarWindowHeight = AccelWindowHeight;
    PolarWindowX = x + w - padding - PolarWindowWidth/2;
    PolarWindowY = y + padding + PolarWindowHeight/2;
    PolarCorner = (sqrt(2)*PolarWindowWidth/2)/2;
  }

  public void screenResized(){
    int prevX = x;
    int prevY = y;
    int prevW = w;
    int prevH = h;

    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    int dy = y - prevY;
    println("dy = " + dy);

    //put your code here...
    // AccelWindowWidth = int(w) - 10;
    // AccelWindowX = int(x)+5;
    // AccelWindowY = int(y)-10+int(h)/2;
    //
    // PolarWindowX = x+AccelWindowWidth-90;
    // PolarWindowY = y+83;
    // PolarCorner = (sqrt(2)*PolarWindowWidth/2)/2;
    println("Acc Widget -- Screen Resized.");

    setGraphDimensions();

    //empty arrays to start redrawing from scratch
    for (int i=0; i<X.length; i++) {  // initialize the accelerometer data
      X[i] = AccelWindowY + AccelWindowHeight/4; // X at 1/4
      Y[i] = AccelWindowY + AccelWindowHeight/2;  // Y at 1/2
      Z[i] = AccelWindowY + (AccelWindowHeight/4)*3;  // Z at 3/4
      // X[i] = X[i] + dy;
      // Y[i] = Y[i] + dy;
      // Z[i] = Z[i] + dy;
    }

    if(eegDataSource == DATASOURCE_GANGLION){
      // accelModeButton.setPos((int)(x + w/2 - accelModeButton.but_dx/2), (int)(y + 80));
      accelModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
    }
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(eegDataSource == DATASOURCE_GANGLION){
      //put your code here...
      if (accelModeButton.isMouseHere()) {
        accelModeButton.setIsActive(true);
      }
    }
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(eegDataSource == DATASOURCE_GANGLION){
      //put your code here...
      if(accelModeButton.isActive && accelModeButton.isMouseHere()){
        println("toggle...");
        if(ganglion.isAccelModeActive()){
          ganglion.accelStop();

          accelModeButton.setString("Turn Accel On");
        } else{
          ganglion.accelStart();
          accelModeButton.setString("Turn Accel Off");
        }
      }
      accelModeButton.setIsActive(false);
    }

  }

  //add custom classes functions here
  public void drawAccValues() {
    textAlign(LEFT,CENTER);
    textFont(h1,20);
    fill(Xcolor);
    text("X = " + nf(currentXvalue, 1, 3) + " g", x+padding , y + (h/12)*1.5f);
    fill(Ycolor);
    text("Y = " + nf(currentYvalue, 1, 3) + " g", x+padding, y + (h/12)*3);
    fill(Zcolor);
    text("Z = " + nf(currentZvalue, 1, 3) + " g", x+padding, y + (h/12)*4.5f);
  }

  public void shiftWave() {
    for (int i = 0; i < X.length-1; i++) {      // move the pulse waveform by
      X[i] = X[i+1];
      Y[i] = Y[i+1];
      Z[i] = Z[i+1];
    }
  }

  public void draw3DGraph() {
    noFill();
    strokeWeight(3);
    stroke(Xcolor);
    line(PolarWindowX, PolarWindowY, PolarWindowX+map(currentXvalue, -yMaxMin, yMaxMin, -PolarWindowWidth/2, PolarWindowWidth/2), PolarWindowY);
    stroke(Ycolor);
    line(PolarWindowX, PolarWindowY, PolarWindowX+map((sqrt(2)*currentYvalue/2), -yMaxMin, yMaxMin, -PolarWindowWidth/2, PolarWindowWidth/2), PolarWindowY+map((sqrt(2)*currentYvalue/2), -yMaxMin, yMaxMin, PolarWindowWidth/2, -PolarWindowWidth/2));
    stroke(Zcolor);
    line(PolarWindowX, PolarWindowY, PolarWindowX, PolarWindowY+map(currentZvalue, -yMaxMin, yMaxMin, PolarWindowWidth/2, -PolarWindowWidth/2));
  }

  public void drawAccWave() {
    noFill();
    strokeWeight(1);
    beginShape();                                  // using beginShape() renders fast
    stroke(Xcolor);
    for (int i = 0; i < X.length; i++) {
      // int xi = int(map(i, 0, X.length-1, 0, AccelWindowWidth-1));
      // vertex(AccelWindowX+xi, X[i]);                    //draw a line connecting the data points
      int xi = PApplet.parseInt(map(i, 0, X.length-1, 0, AccelWindowWidth-1));
      // int yi = int(map(X[i], yMaxMin, -yMaxMin, 0.0, AccelWindowHeight-1));
      // int yi = 2;
      vertex(AccelWindowX+xi, X[i]);                    //draw a line connecting the data points
    }
    endShape();

    beginShape();
    stroke(Ycolor);
    for (int i = 0; i < Y.length; i++) {
      int xi = PApplet.parseInt(map(i, 0, X.length-1, 0, AccelWindowWidth-1));
      vertex(AccelWindowX+xi, Y[i]);
    }
    endShape();

    beginShape();
    stroke(Zcolor);
    for (int i = 0; i < Z.length; i++) {
      int xi = PApplet.parseInt(map(i, 0, X.length-1, 0, AccelWindowWidth-1));
      vertex(AccelWindowX+xi, Z[i]);
    }
    endShape();
  }

  public void drawAccWave2() {
    noFill();
    strokeWeight(1);
    beginShape();                                  // using beginShape() renders fast
    stroke(Xcolor);
    for (int i = 0; i < accelerometerBuff[0].length; i++) {
      int x = PApplet.parseInt(map(accelerometerBuff[0][i], -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));  // ss
      x = constrain(x, AccelWindowY, AccelWindowY+AccelWindowHeight);
      vertex(AccelWindowX+i, x);                    //draw a line connecting the data points
    }
    endShape();

    beginShape();
    stroke(Ycolor);
    for (int i = 0; i < accelerometerBuff[0].length; i++) {
      int y = PApplet.parseInt(map(accelerometerBuff[1][i], -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));  // ss
      y = constrain(y, AccelWindowY, AccelWindowY+AccelWindowHeight);
      vertex(AccelWindowX+i, y);
    }
    endShape();

    beginShape();
    stroke(Zcolor);
    for (int i = 0; i < accelerometerBuff[0].length; i++) {
      int z = PApplet.parseInt(map(accelerometerBuff[2][i], -yMaxMin, yMaxMin, PApplet.parseFloat(AccelWindowY+AccelWindowHeight), PApplet.parseFloat(AccelWindowY)));  // ss
      z = constrain(z, AccelWindowY, AccelWindowY+AccelWindowHeight);
      vertex(AccelWindowX+i, z);
    }
    endShape();
  }

  public void synthesizeAccelerometerData() {
    if (Xrising) {  // MAKE A SAW WAVE FOR TESTING
      X[X.length-1]--;   // place the new raw datapoint at the end of the array
      if (X[X.length-1] <= AccelWindowY) {
        Xrising = false;
      }
    } else {
      X[X.length-1]++;   // place the new raw datapoint at the end of the array
      if (X[X.length-1] >= AccelWindowY+AccelWindowHeight) {
        Xrising = true;
      }
    }

    if (Yrising) {  // MAKE A SAW WAVE FOR TESTING
      Y[Y.length-1]--;   // place the new raw datapoint at the end of the array
      if (Y[Y.length-1] <= AccelWindowY) {
        Yrising = false;
      }
    } else {
      Y[Y.length-1]++;   // place the new raw datapoint at the end of the array
      if (Y[Y.length-1] >= AccelWindowY+AccelWindowHeight) {
        Yrising = true;
      }
    }

    if (Zrising) {  // MAKE A SAW WAVE FOR TESTING
      Z[Z.length-1]--;   // place the new raw datapoint at the end of the array
      if (Z[Z.length-1] <= AccelWindowY) {
        Zrising = false;
      }
    } else {
      Z[Z.length-1]++;   // place the new raw datapoint at the end of the array
      if (Z[Z.length-1] >= AccelWindowY+AccelWindowHeight) {
        Zrising = true;
      }
    }
  }

};

// //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
// void Thisdrop(int n){
//   println("Item " + (n+1) + " selected from Dropdown 1");
//   if(n==0){
//     //do this
//   } else if(n==1){
//     //do this instead
//   }
//
//   closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
// }
//
// void Dropdown2(int n){
//   println("Item " + (n+1) + " selected from Dropdown 2");
//   closeAllDropdowns();
// }
//
// void Dropdown3(int n){
//   println("Item " + (n+1) + " selected from Dropdown 3");
//   closeAllDropdowns();
// }

////////////////////////////////////////////////////
//
//    W_template.pde (ie "Widget Template")
//
//    This is a Template Widget, intended to be used as a starting point for OpenBCI Community members that want to develop their own custom widgets!
//    Good luck! If you embark on this journey, please let us know. Your contributions are valuable to everyone!
//
//    Created by: Conor Russomanno, November 2016
//
///////////////////////////////////////////////////,

class W_ganglionImpedance extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  Button startStopCheck;
  int padding = 24;

  W_ganglionImpedance(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    // addDropdown("Dropdown1", "Drop 1", Arrays.asList("A", "B"), 0);
    // addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    // addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

    startStopCheck = new Button (x + padding, y + padding, 200, navHeight, "Start Impedance Check", 12);
    startStopCheck.setFont(p4, 14);

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

    startStopCheck.draw();

    // //without dividing by 2
    // for(int i = 0; i < ganglion.impedanceArray.length; i++){
    //   String toPrint;
    //   if(i == 0){
    //     toPrint = "Reference Impedance = " + ganglion.impedanceArray[i] + " k\u2126";
    //   } else {
    //     toPrint = "Channel[" + i + "] Impedance = " + ganglion.impedanceArray[i] + " k\u2126";
    //   }
    //   text(toPrint, x + 10, y + 60 + 20*(i));
    // }

    //divide by 2 ... we do this assuming that the D_G (driven ground) electrode is "comprable in impedance" to the electrode being used.
    fill(bgColor);
    textFont(p4, 14);
    for(int i = 0; i < ganglion.impedanceArray.length; i++){
      String toPrint;
      float adjustedImpedance = ganglion.impedanceArray[i]/2.0f;
      if(i == 0){
        toPrint = "Reference Impedance \u2248 " + adjustedImpedance + " k\u2126";
      } else {
        toPrint = "Channel[" + i + "] Impedance \u2248 " + adjustedImpedance + " k\u2126";
      }
      text(toPrint, x + padding + 40, y + padding*2 + 12 + startStopCheck.but_dy + padding*(i));

      pushStyle();
      stroke(bgColor);
      //change the fill color based on the signal quality...
      if(adjustedImpedance <= 0){ //no data yet...
        fill(255);
      } else if(adjustedImpedance > 0 && adjustedImpedance <= 10){ //very good signal quality
        fill(49, 113, 89); //dark green
      } else if(adjustedImpedance > 10 && adjustedImpedance <= 50){ //good signal quality
        fill(184, 220, 105); //yellow green
      } else if(adjustedImpedance > 50 && adjustedImpedance <= 100){ //acceptable signal quality
        fill(221, 178, 13); //yellow
      } else if(adjustedImpedance > 100 && adjustedImpedance <= 150){ //questionable signal quality
        fill(253, 94, 52); //orange
      } else if(adjustedImpedance > 150){ //bad signal quality
        fill(224, 56, 45); //red
      }

      ellipse(x + padding + 10, y + padding*2 + 7 + startStopCheck.but_dy + padding*(i), padding/2, padding/2);
      popStyle();
    }

    if(isHubInitialized && isGanglionObjectInitialized && eegDataSource == DATASOURCE_GANGLION){
      if(ganglion.isCheckingImpedance()){
        image(loadingGIF_blue, x + padding + startStopCheck.but_dx + 15, y + padding - 8, 40, 40);
      }
    }

    // // no longer need to do this because the math was moved to the firmware...
    // for(int i = 0; i < ganglion.impedanceArray.length; i++){
    //   String toPrint;
    //   float target = convertRawGanglionImpedanceToTarget(ganglion.impedanceArray[i]/1000.0);
    //   if(i == 0){
    //     toPrint = "Reference Impedance = " + target + " k\u2126";
    //   } else {
    //     toPrint = "Channel[" + i + "] Impedance = " + target + " k\u2126";
    //   }
    //   text(toPrint, x + 10, y + 220 + 20*(i));
    // }

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    startStopCheck.setPos(x + padding, y + padding);

  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(startStopCheck.isMouseHere()){
      startStopCheck.setIsActive(true);
    }

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(startStopCheck.isActive && startStopCheck.isMouseHere()){
      if(isHubInitialized && isGanglionObjectInitialized && eegDataSource == DATASOURCE_GANGLION){
        if(ganglion.isCheckingImpedance()){
          ganglion.impedanceStop();
          startStopCheck.but_txt = "Start Impedance Check";
        } else {
          ganglion.impedanceStart();
          startStopCheck.but_txt = "Stop Impedance Check";

          // if is running... stopRunning and switch the state of the Start/Stop button back to Data Stream stopped
          stopRunning();
          topNav.stopButton.setString(topNav.stopButton_pressToStart_txt);
          topNav.stopButton.setColorNotPressed(color(184, 220, 105));

        }
      }
    }
    startStopCheck.setIsActive(false);

  }

  //add custom classes functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget
  }

};

public float convertRawGanglionImpedanceToTarget(float _actual){

  //the following impedance adjustment calculations were derived using empirical values from resistors between 1,2,3,4,REF-->D_G
  float _target;

  //V1 -- more accurate for lower impedances (< 22kOhcm) -> y = 0.0034x^3 - 0.1443x^2 + 3.1324x - 10.59
  if(_actual <= 22){
    // _target = (0.0004)*(pow(_actual,3)) - (0.0262)*(pow(_actual,2)) + (1.8349)*(_actual) - 6.6006;
    _target = (0.0034f)*(pow(_actual,3)) - (0.1443f)*(pow(_actual,2)) + (3.1324f)*(_actual) - 10.59f;
  }
  //V2 -- more accurate for higher impedances (> 22kOhm) -> y = 0.000009x^4 - 0.001x^3 + 0.0409x^2 + 0.6445x - 1
  else {
    _target = (0.000009f)*(pow(_actual,4)) - (0.001f)*pow(_actual,3) + (0.0409f)*(pow(_actual,2)) + (0.6445f)*(pow(_actual,1)) - 1;
  }

  return _target;

}

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
// void Dropdown1(int n){
//   println("Item " + (n+1) + " selected from Dropdown 1");
//   if(n==0){
//     //do this
//   } else if(n==1){
//     //do this instead
//   }
//
//   closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
// }
//
// void Dropdown2(int n){
//   println("Item " + (n+1) + " selected from Dropdown 2");
//   closeAllDropdowns();
// }
//
// void Dropdown3(int n){
//   println("Item " + (n+1) + " selected from Dropdown 3");
//   closeAllDropdowns();
// }

////////////////////////////////////////////////////
//
//    W_template.pde (ie "Widget Template")
//
//    This is a Template Widget, intended to be used as a starting point for OpenBCI Community members that want to develop their own custom widgets!
//    Good luck! If you embark on this journey, please let us know. Your contributions are valuable to everyone!
//
//    Created by: Conor Russomanno, November 2016
//    Based on code written by: Chip Audette, Oct 2013
//
///////////////////////////////////////////////////,

float[] smoothFac = new float[]{0.0f, 0.5f, 0.75f, 0.9f, 0.95f, 0.98f}; //used by FFT & Headplot
int smoothFac_ind = 3;    //initial index into the smoothFac array = 0.75 to start .. used by FFT & Head Plots
int intensityFac_ind = 2;

class W_headPlot extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  HeadPlot headPlot;

  W_headPlot(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    // addDropdown("Ten20", "Layout", Arrays.asList("10-20", "5-10"), 0);
    // addDropdown("Headset", "Headset", Arrays.asList("None", "Mark II", "Mark III", "Mark IV "), 0);
    addDropdown("Intensity", "Intensity", Arrays.asList("4x", "2x", "1x", "0.5x", "0.2x", "0.02x"), vertScaleFactor_ind);
    addDropdown("Polarity", "Polarity", Arrays.asList("+/-", " + "), 0);
    addDropdown("ShowContours", "Contours", Arrays.asList("ON", "OFF"), 0);
    addDropdown("SmoothingHeadPlot", "Smooth", Arrays.asList("0.0", "0.5", "0.75", "0.9", "0.95", "0.98"), smoothFac_ind);

    //add your code here
    headPlot = new HeadPlot(x, y, w, h, win_x, win_y);
    //FROM old Gui_Manager
    headPlot.setIntensityData_byRef(dataProcessing.data_std_uV, is_railed);
    headPlot.setPolarityData_byRef(dataProcessing.polarity);
    setSmoothFac(smoothFac[smoothFac_ind]);

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    headPlot.update();
  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here
    headPlot.draw(); //draw the actual headplot

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    headPlot.setPositionSize(x, y, w, h, width, height);     //update position of headplot

  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  //add custom class functions here
  public void setSmoothFac(float fac) {
    headPlot.smooth_fac = fac;
  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void Ten20(int n) { //triggered when there is an event in the Ten20 Dropdown
  /* here an item is stored as a Map  with the following key-value pairs:
   * name, the given name of the item
   * text, the given text of the item by default the same as name
   * value, the given value of the item, can be changed by using .getItem(n).put("value", "abc"); a value here is of type Object therefore can be anything
   * color, the given color of the item, how to change, see below
   * view, a customizable view, is of type CDrawable
   */

  //fft_widget.fft_plot.setXLim(0.1, fft_widget.xLimOptions[n]); //update the xLim of the FFT_Plot
  println("BOOOOM!" + n);
  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close

}

//triggered when there is an event in the Headset Dropdown
public void Headset(int n) {
  //fft_widget.fft_plot.setYLim(0.1, fft_widget.yLimOptions[n]); //update the yLim of the FFT_Plot
  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

//triggered when there is an event in the Polarity Dropdown
public void Polarity(int n) {

  if (n==0) {
    w_headPlot.headPlot.use_polarity = true;
  } else {
    w_headPlot.headPlot.use_polarity = false;
  }
  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

public void ShowContours(int n){
  if(n==0){
    //turn headplot contours on
    w_headPlot.headPlot.drawHeadAsContours = true;
  } else if(n==1){
    //turn headplot contours off
    w_headPlot.headPlot.drawHeadAsContours = false;
  }
  closeAllDropdowns();
}

//triggered when there is an event in the SmoothingHeadPlot Dropdown
public void SmoothingHeadPlot(int n) {
  w_headPlot.setSmoothFac(smoothFac[n]);
  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

//triggered when there is an event in the UnfiltFiltHeadPlot Dropdown
public void UnfiltFiltHeadPlot(int n) {
  //currently not in use
  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

public void Intensity(int n){
  vertScaleFactor_ind = n;
  updateVertScale();
  closeAllDropdowns();
}

// ----- these variable/methods are used for adjusting the intensity factor of the headplot opacity ---------------------------------------------------------------------------------------------------------
float default_vertScale_uV = 200.0f; //this defines the Y-scale on the montage plots...this is the vertical space between traces
float[] vertScaleFactor = { 0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 50.0f};
int vertScaleFactor_ind = 2;
float vertScale_uV = default_vertScale_uV;

public void setVertScaleFactor_ind(int ind) {
  vertScaleFactor_ind = max(0,ind);
  if (ind >= vertScaleFactor.length) vertScaleFactor_ind = 0;
  updateVertScale();
}

public void updateVertScale() {
  vertScale_uV = default_vertScale_uV * vertScaleFactor[vertScaleFactor_ind];
  w_headPlot.headPlot.setMaxIntensity_uV(vertScale_uV);
}
//---------------------------------------------------------------------------------------------------------------------------------------

//////////////////////////////////////////////////////////////
//
// HeadPlot Class
//
// This class creates and manages the head-shaped plot used by the GUI.
// The head includes circles representing the different EEG electrodes.
// The color (brightness) of the electrodes can be adjusted so that the
// electrodes' brightness values dynamically reflect the intensity of the
// EEG signal.  All EEG processing must happen outside of this class.
//
// Created by: Chip Audette 2013
//
///////////////////////////////////////////////////////////////

// Note: This routine uses aliasing to know which data should be used to
// set the brightness of the electrodes.

class HeadPlot {
  private float rel_posX, rel_posY, rel_width, rel_height;
  private int circ_x, circ_y, circ_diam;
  private int earL_x, earL_y, earR_x, earR_y, ear_width, ear_height;
  private int[] nose_x, nose_y;
  private float[][] electrode_xy;
  private float[] ref_electrode_xy;
  private float[][][] electrode_color_weightFac;
  private int[][] electrode_rgb;
  private float[][] headVoltage;
  private int elec_diam;
  PFont font;
  public float[] intensity_data_uV;
  public float[] polarity_data;
  private DataStatus[] is_railed;
  private float intense_min_uV=0.0f, intense_max_uV=1.0f, assumed_railed_voltage_uV=1.0f;
  private float log10_intense_min_uV = 0.0f, log10_intense_max_uV=1.0f;
  PImage headImage;
  private int image_x, image_y;
  public boolean drawHeadAsContours;
  private boolean plot_color_as_log = true;
  public float smooth_fac = 0.0f;
  private boolean use_polarity = true;

  HeadPlot(float x, float y, float w, float h, int win_x, int win_y, int n) {
    final int n_elec = n;  //8 electrodes assumed....or 16 for 16-channel?  Change this!!!
    nose_x = new int[3];
    nose_y = new int[3];
    electrode_xy = new float[n_elec][2];   //x-y position of electrodes (pixels?)
    //electrode_relDist = new float[n_elec][n_elec];  //relative distance between electrodes (pixels)
    ref_electrode_xy = new float[2];  //x-y position of reference electrode
    electrode_rgb = new int[3][n_elec];  //rgb color for each electrode
    font = createFont("Arial", 16);
    drawHeadAsContours = true; //set this to be false for slower computers

    rel_posX = x;
    rel_posY = y;
    rel_width = w;
    rel_height = h;
    setWindowDimensions(win_x, win_y);

    setMaxIntensity_uV(200.0f);  //default intensity scaling for electrodes
  }

  HeadPlot(int _x, int _y, int _w, int _h, int _win_x, int _win_y) {
    final int n_elec = nchan;  //8 electrodes assumed....or 16 for 16-channel?  Change this!!!
    nose_x = new int[3];
    nose_y = new int[3];
    electrode_xy = new float[n_elec][2];   //x-y position of electrodes (pixels?)
    //electrode_relDist = new float[n_elec][n_elec];  //relative distance between electrodes (pixels)
    ref_electrode_xy = new float[2];  //x-y position of reference electrode
    electrode_rgb = new int[3][n_elec];  //rgb color for each electrode
    font = p5;
    drawHeadAsContours = true; //set this to be false for slower computers

    //float percentMargin = 0.1;
    //_x = _x + (int)(float(_w)*percentMargin);
    //_y = _y + (int)(float(_h)*percentMargin);
    //_w = (int)(float(_w)-(2*(float(_w)*percentMargin)));
    //_h = (int)(float(_h)-(2*(float(_h)*percentMargin)));

    //rel_posX = float(_x)/_win_x;
    //rel_posY = float(_y)/_win_y;
    //rel_width = float(_w)/_win_x;
    //rel_height = float(_h)/_win_y;
    //setWindowDimensions(_win_x, _win_y);

    setPositionSize(_x, _y, _w, _h, _win_x, _win_y);
    setMaxIntensity_uV(200.0f);  //default intensity scaling for electrodes
  }

  public void setPositionSize(int _x, int _y, int _w, int _h, int _win_x, int _win_y) {
    float percentMargin = 0.1f;
    _x = _x + (int)(PApplet.parseFloat(_w)*percentMargin);
    _y = _y + (int)(PApplet.parseFloat(_h)*percentMargin)-navHeight/2;
    _w = (int)(PApplet.parseFloat(_w)-(2*(PApplet.parseFloat(_w)*percentMargin)));
    _h = (int)(PApplet.parseFloat(_h)-(2*(PApplet.parseFloat(_h)*percentMargin)));

    rel_posX = PApplet.parseFloat(_x)/_win_x;
    rel_posY = PApplet.parseFloat(_y)/_win_y;
    rel_width = PApplet.parseFloat(_w)/_win_x;
    rel_height = PApplet.parseFloat(_h)/_win_y;
    setWindowDimensions(_win_x, _win_y);
  }

  public void setIntensityData_byRef(float[] data, DataStatus[] is_rail) {
    intensity_data_uV = data;  //simply alias the data held externally.  DOES NOT COPY THE DATA ITSEF!  IT'S SIMPLY LINKED!
    is_railed = is_rail;
  }

  public void setPolarityData_byRef(float[] data) {
    polarity_data = data;//simply alias the data held externally.  DOES NOT COPY THE DATA ITSEF!  IT'S SIMPLY LINKED!
    //if (polarity_data != null) use_polarity = true;
  }

  public String getUsePolarityTrueFalse() {
    if (use_polarity) {
      return "True";
    } else {
      return "False";
    }
  }

  public void setMaxIntensity_uV(float val_uV) {
    intense_max_uV = val_uV;
    intense_min_uV = intense_max_uV / 200.0f * 5.0f;  //set to 200, get 5
    assumed_railed_voltage_uV = intense_max_uV;

    log10_intense_max_uV = log10(intense_max_uV);
    log10_intense_min_uV = log10(intense_min_uV);
  }

  public void set_plotColorAsLog(boolean state) {
    plot_color_as_log = state;
  }

  //this method defines all locations of all the subcomponents
  public void setWindowDimensions(int win_width, int win_height) {
    final int n_elec = electrode_xy.length;

    //define the head itself
    float nose_relLen = 0.075f;
    float nose_relWidth = 0.05f;
    float nose_relGutter = 0.02f;
    float ear_relLen = 0.15f;
    float ear_relWidth = 0.075f;

    float square_width = min(rel_width*(float)win_width,
      rel_height*(float)win_height);  //choose smaller of the two

    float total_width = square_width;
    float total_height = square_width;
    float nose_width = total_width * nose_relWidth;
    float nose_height = total_height * nose_relLen;
    ear_width = (int)(ear_relWidth * total_width);
    ear_height = (int)(ear_relLen * total_height);
    int circ_width_foo = (int)(total_width - 2.f*((float)ear_width)/2.0f);
    int circ_height_foo = (int)(total_height - nose_height);
    circ_diam = min(circ_width_foo, circ_height_foo);
    //println("headPlot: circ_diam: " + circ_diam);

    //locations: circle center, measured from upper left
    circ_x = (int)((rel_posX+0.5f*rel_width)*(float)win_width);                  //center of head
    circ_y = (int)((rel_posY+0.5f*rel_height)*(float)win_height + nose_height);  //center of head

    //locations: ear centers, measured from upper left
    earL_x = circ_x - circ_diam/2;
    earR_x = circ_x + circ_diam/2;
    earL_y = circ_y;
    earR_y = circ_y;

    //locations nose vertexes, measured from upper left
    nose_x[0] = circ_x - (int)((nose_relWidth/2.f)*(float)win_width);
    nose_x[1] = circ_x + (int)((nose_relWidth/2.f)*(float)win_width);
    nose_x[2] = circ_x;
    nose_y[0] = circ_y - (int)((float)circ_diam/2.0f - nose_relGutter*(float)win_height);
    nose_y[1] = nose_y[0];
    nose_y[2] = circ_y - (int)((float)circ_diam/2.0f + nose_height);


    //define the electrode positions as the relative position [-1.0 +1.0] within the head
    //remember that negative "Y" is up and positive "Y" is down
    float elec_relDiam = 0.12f; //was 0.1425 prior to 2014-03-23
    elec_diam = (int)(elec_relDiam*((float)circ_diam));
    setElectrodeLocations(n_elec, elec_relDiam);

    //define image to hold all of this
    image_x = PApplet.parseInt(round(circ_x - 0.5f*circ_diam - 0.5f*ear_width));
    image_y = nose_y[2];
    headImage = createImage(PApplet.parseInt(total_width), PApplet.parseInt(total_height), ARGB);

    //initialize the image
    for (int Iy=0; Iy < headImage.height; Iy++) {
      for (int Ix = 0; Ix < headImage.width; Ix++) {
        headImage.set(Ix, Iy, color(0, 0, 0, 0));
      }
    }

    //define the weighting factors to go from the electrode voltages
    //outward to the full the contour plot
    if (false) {
      //here is a simple distance-based algorithm that works every time, though
      //is not really physically accurate.  It looks decent enough
      computePixelWeightingFactors();
    } else {
      //here is the better solution that is more physical.  It involves an iterative
      //solution, which could be really slow or could fail.  If it does poorly,
      //switch to using the algorithm above.
      int n_wide_full = PApplet.parseInt(total_width);
      int n_tall_full = PApplet.parseInt(total_height);
      computePixelWeightingFactors_multiScale(n_wide_full, n_tall_full);
    }
  } //end of method


  private void setElectrodeLocations(int n_elec, float elec_relDiam) {
    //try loading the positions from a file
    int n_elec_to_load = n_elec+1;  //load the n_elec plus the reference electrode
    Table elec_relXY = new Table();
    String default_fname = "electrode_positions_default.txt";
    //String default_fname = "electrode_positions_12elec_scalp9.txt";
    try {
      elec_relXY = loadTable(default_fname, "header,csv"); //try loading the default file
    }
    catch (NullPointerException e) {
    };

    //get the default locations if the file didn't exist
    if ((elec_relXY == null) || (elec_relXY.getRowCount() < n_elec_to_load)) {
      println("headPlot: electrode position file not found or was wrong size: " + default_fname);
      println("        : using defaults...");
      elec_relXY = createDefaultElectrodeLocations(default_fname, elec_relDiam);
    }

    //define the actual locations of the electrodes in pixels
    for (int i=0; i < min(electrode_xy.length, elec_relXY.getRowCount()); i++) {
      electrode_xy[i][0] = circ_x+(int)(elec_relXY.getFloat(i, 0)*((float)circ_diam));
      electrode_xy[i][1] = circ_y+(int)(elec_relXY.getFloat(i, 1)*((float)circ_diam));
    }

    //the referenece electrode is last in the file
    ref_electrode_xy[0] = circ_x+(int)(elec_relXY.getFloat(elec_relXY.getRowCount()-1, 0)*((float)circ_diam));
    ref_electrode_xy[1] = circ_y+(int)(elec_relXY.getFloat(elec_relXY.getRowCount()-1, 1)*((float)circ_diam));
  }

  private Table createDefaultElectrodeLocations(String fname, float elec_relDiam) {

    //regular electrodes
    float[][] elec_relXY = new float[16][2];
    elec_relXY[0][0] = -0.125f;
    elec_relXY[0][1] = -0.5f + elec_relDiam*(0.5f+0.2f); //FP1
    elec_relXY[1][0] = -elec_relXY[0][0];
    elec_relXY[1][1] = elec_relXY[0][1]; //FP2

    elec_relXY[2][0] = -0.2f;
    elec_relXY[2][1] = 0f; //C3
    elec_relXY[3][0] = -elec_relXY[2][0];
    elec_relXY[3][1] = elec_relXY[2][1]; //C4

    elec_relXY[4][0] = -0.3425f;
    elec_relXY[4][1] = 0.27f; //T5 (aka P7)
    elec_relXY[5][0] = -elec_relXY[4][0];
    elec_relXY[5][1] = elec_relXY[4][1]; //T6 (aka P8)

    elec_relXY[6][0] = -0.125f;
    elec_relXY[6][1] = +0.5f - elec_relDiam*(0.5f+0.2f); //O1
    elec_relXY[7][0] = -elec_relXY[6][0];
    elec_relXY[7][1] = elec_relXY[6][1];  //O2

    elec_relXY[8][0] = elec_relXY[4][0];
    elec_relXY[8][1] = -elec_relXY[4][1]; //F7
    elec_relXY[9][0] = -elec_relXY[8][0];
    elec_relXY[9][1] = elec_relXY[8][1]; //F8

    elec_relXY[10][0] = -0.18f;
    elec_relXY[10][1] = -0.15f; //C3
    elec_relXY[11][0] = -elec_relXY[10][0];
    elec_relXY[11][1] = elec_relXY[10][1]; //C4

    elec_relXY[12][0] =  -0.5f +elec_relDiam*(0.5f+0.15f);
    elec_relXY[12][1] = 0f; //T3 (aka T7?)
    elec_relXY[13][0] = -elec_relXY[12][0];
    elec_relXY[13][1] = elec_relXY[12][1]; //T4 (aka T8)

    elec_relXY[14][0] = elec_relXY[10][0];
    elec_relXY[14][1] = -elec_relXY[10][1]; //CP3
    elec_relXY[15][0] = -elec_relXY[14][0];
    elec_relXY[15][1] = elec_relXY[14][1]; //CP4

    //reference electrode
    float[] ref_elec_relXY = new float[2];
    ref_elec_relXY[0] = 0.0f;
    ref_elec_relXY[1] = 0.0f;

    //put it all into a table
    Table table_elec_relXY = new Table();
    table_elec_relXY.addColumn("X", Table.FLOAT);
    table_elec_relXY.addColumn("Y", Table.FLOAT);
    for (int I = 0; I < elec_relXY.length; I++) {
      table_elec_relXY.addRow();
      table_elec_relXY.setFloat(I, "X", elec_relXY[I][0]);
      table_elec_relXY.setFloat(I, "Y", elec_relXY[I][1]);
    }

    //last one is the reference electrode
    table_elec_relXY.addRow();
    table_elec_relXY.setFloat(table_elec_relXY.getRowCount()-1, "X", ref_elec_relXY[0]);
    table_elec_relXY.setFloat(table_elec_relXY.getRowCount()-1, "Y", ref_elec_relXY[1]);

    //try writing it to a file
    String full_fname = "Data\\" + fname;
    try {
      saveTable(table_elec_relXY, full_fname, "csv");
    }
    catch (NullPointerException e) {
      println("headPlot: createDefaultElectrodeLocations: could not write file to " + full_fname);
    };

    //return
    return table_elec_relXY;
  } //end of method

  //Here, we do a two-step solution to get the weighting factors.
  //We do a coarse grid first.  We do our iterative solution on the coarse grid.
  //Then, we formulate the full resolution fine grid.  We interpolate these points
  //from the data resulting from the coarse grid.
  private void computePixelWeightingFactors_multiScale(int n_wide_full, int n_tall_full) {
    int n_elec = electrode_xy.length;

    //define the coarse grid data structures and pixel locations
    int decimation = 10;
    int n_wide_small = n_wide_full / decimation + 1;
    int n_tall_small = n_tall_full / decimation + 1;
    float weightFac[][][] = new float[n_elec][n_wide_small][n_tall_small];
    int pixelAddress[][][] = new int[n_wide_small][n_tall_small][2];
    for (int Ix=0; Ix<n_wide_small; Ix++) {
      for (int Iy=0; Iy<n_tall_small; Iy++) {
        pixelAddress[Ix][Iy][0] = Ix*decimation;
        pixelAddress[Ix][Iy][1] = Iy*decimation;
      };
    };

    //compute the weighting factors of the coarse grid
    computePixelWeightingFactors_trueAverage(pixelAddress, weightFac);

    //define the fine grid data structures
    electrode_color_weightFac = new float[n_elec][n_wide_full][n_tall_full];
    headVoltage = new float[n_wide_full][n_tall_full];

    //interpolate to get the fine grid from the coarse grid
    float dx_frac, dy_frac;
    for (int Ix=0; Ix<n_wide_full; Ix++) {
      int Ix_source = Ix/decimation;
      dx_frac = PApplet.parseFloat(Ix - Ix_source*decimation)/PApplet.parseFloat(decimation);
      for (int Iy=0; Iy < n_tall_full; Iy++) {
        int Iy_source = Iy/decimation;
        dy_frac = PApplet.parseFloat(Iy - Iy_source*decimation)/PApplet.parseFloat(decimation);

        for (int Ielec=0; Ielec<n_elec; Ielec++) {
          //println("    : Ielec = " + Ielec);
          if ((Ix_source < (n_wide_small-1)) && (Iy_source < (n_tall_small-1))) {
            //normal 2-D interpolation
            electrode_color_weightFac[Ielec][Ix][Iy] = interpolate2D(weightFac[Ielec], Ix_source, Iy_source, Ix_source+1, Iy_source+1, dx_frac, dy_frac);
          } else if (Ix_source < (n_wide_small-1)) {
            //1-D interpolation in X
            dy_frac = 0.0f;
            electrode_color_weightFac[Ielec][Ix][Iy] = interpolate2D(weightFac[Ielec], Ix_source, Iy_source, Ix_source+1, Iy_source, dx_frac, dy_frac);
          } else if (Iy_source < (n_tall_small-1)) {
            //1-D interpolation in Y
            dx_frac = 0.0f;
            electrode_color_weightFac[Ielec][Ix][Iy] = interpolate2D(weightFac[Ielec], Ix_source, Iy_source, Ix_source, Iy_source+1, dx_frac, dy_frac);
          } else {
            //no interpolation, just use the last value
            electrode_color_weightFac[Ielec][Ix][Iy] = weightFac[Ielec][Ix_source][Iy_source];
          }  //close the if block selecting the interpolation configuration
        } //close Ielec loop
      } //close Iy loop
    } // close Ix loop

    //clean up the boundaries of our interpolated results to make the look nicer
    int pixelAddress_full[][][] = new int[n_wide_full][n_tall_full][2];
    for (int Ix=0; Ix<n_wide_full; Ix++) {
      for (int Iy=0; Iy<n_tall_full; Iy++) {
        pixelAddress_full[Ix][Iy][0] = Ix;
        pixelAddress_full[Ix][Iy][1] = Iy;
      };
    };
    cleanUpTheBoundaries(pixelAddress_full, electrode_color_weightFac);
  } //end of method


  private float interpolate2D(float[][] weightFac, int Ix1, int Iy1, int Ix2, int Iy2, float dx_frac, float dy_frac) {
    if (Ix1 >= weightFac.length) {
      println("headPlot: interpolate2D: Ix1 = " + Ix1 + ", weightFac.length = " + weightFac.length);
    }
    float foo1 = (weightFac[Ix2][Iy1] - weightFac[Ix1][Iy1])*dx_frac + weightFac[Ix1][Iy1];
    float foo2 = (weightFac[Ix2][Iy2] - weightFac[Ix1][Iy2])*dx_frac + weightFac[Ix1][Iy2];
    return (foo2 - foo1) * dy_frac + foo1;
  }


  //here is the simpler and more robust algorithm.  It's not necessarily physically real, though.
  //but, it will work every time.  So, if the other method fails, go with this one.
  private void computePixelWeightingFactors() {
    int n_elec = electrode_xy.length;
    float dist;
    int withinElecInd = -1;
    float elec_radius = 0.5f*elec_diam;
    int pixel_x, pixel_y;
    float sum_weight_fac = 0.0f;
    float weight_fac[] = new float[n_elec];
    float foo_dist;

    //loop over each pixel
    for (int Iy=0; Iy < headImage.height; Iy++) {
      pixel_y = image_y + Iy;
      for (int Ix = 0; Ix < headImage.width; Ix++) {
        pixel_x = image_x + Ix;

        if (isPixelInsideHead(pixel_x, pixel_y)==false) {
          for (int Ielec=0; Ielec < n_elec; Ielec++) {
            //outside of head...no color from electrodes
            electrode_color_weightFac[Ielec][Ix][Iy]= -1.0f; //a negative value will be a flag that it is outside of the head
          }
        } else {
          //inside of head, compute weighting factors

          //compute distances of this pixel to each electrode
          sum_weight_fac = 0.0f; //reset for this pixel
          withinElecInd = -1;    //reset for this pixel
          for (int Ielec=0; Ielec < n_elec; Ielec++) {
            //compute distance
            dist = max(1.0f, calcDistance(pixel_x, pixel_y, electrode_xy[Ielec][0], electrode_xy[Ielec][1]));
            if (dist < elec_radius) withinElecInd = Ielec;

            //compute the first part of the weighting factor
            foo_dist = max(1.0f, abs(dist - elec_radius));  //remove radius of the electrode
            weight_fac[Ielec] = 1.0f/foo_dist;  //arbitrarily chosen
            weight_fac[Ielec] = weight_fac[Ielec]*weight_fac[Ielec]*weight_fac[Ielec];  //again, arbitrary
            sum_weight_fac += weight_fac[Ielec];
          }

          //finalize the weight factor
          for (int Ielec=0; Ielec < n_elec; Ielec++) {
            //is this pixel within an electrode?
            if (withinElecInd > -1) {
              //yes, it is within an electrode
              if (Ielec == withinElecInd) {
                //use this signal electrode as the color
                electrode_color_weightFac[Ielec][Ix][Iy] = 1.0f;
              } else {
                //ignore all other electrodes
                electrode_color_weightFac[Ielec][Ix][Iy] = 0.0f;
              }
            } else {
              //no, this pixel is not in an electrode.  So, use the distance-based weight factor,
              //after dividing by the sum of the weight factors, resulting in an averaging operation
              electrode_color_weightFac[Ielec][Ix][Iy] = weight_fac[Ielec]/sum_weight_fac;
            }
          }
        }
      }
    }
  } //end of method

  public void computePixelWeightingFactors_trueAverage(int pixelAddress[][][], float weightFac[][][]) {
    int n_wide = pixelAddress.length;
    int n_tall = pixelAddress[0].length;
    int n_elec = electrode_xy.length;
    int withinElectrode[][] = new int[n_wide][n_tall]; //which electrode is this pixel within (-1 means that it is not within any electrode)
    boolean withinHead[][] = new boolean[n_wide][n_tall]; //is the pixel within the head?
    int toPixels[][][][] = new int[n_wide][n_tall][4][2];
    int toElectrodes[][][] = new int[n_wide][n_tall][4];
    //int numConnections[][] = new int[n_wide][n_tall];

    //find which pixesl are within the head and which pixels are within an electrode
    whereAreThePixels(pixelAddress, withinHead, withinElectrode);

    //loop over the pixels and make all the connections
    makeAllTheConnections(withinHead, withinElectrode, toPixels, toElectrodes);

    //compute the pixel values when lighting up each electrode invididually
    for (int Ielec=0; Ielec<n_elec; Ielec++) {
      computeWeightFactorsGivenOneElectrode_iterative(toPixels, toElectrodes, Ielec, weightFac);
    }
  }

  private void cleanUpTheBoundaries(int pixelAddress[][][], float weightFac[][][]) {
    int n_wide = pixelAddress.length;
    int n_tall = pixelAddress[0].length;
    int n_elec = electrode_xy.length;
    int withinElectrode[][] = new int[n_wide][n_tall]; //which electrode is this pixel within (-1 means that it is not within any electrode)
    boolean withinHead[][] = new boolean[n_wide][n_tall]; //is the pixel within the head?

    //find which pixesl are within the head and which pixels are within an electrode
    whereAreThePixels(pixelAddress, withinHead, withinElectrode);

    //loop over the pixels and change the weightFac to reflext where it is
    for (int Ix=0; Ix<n_wide; Ix++) {
      for (int Iy=0; Iy<n_tall; Iy++) {
        if (withinHead[Ix][Iy]==false) {
          //this pixel is outside of the head
          for (int Ielec=0; Ielec<n_elec; Ielec++) {
            weightFac[Ielec][Ix][Iy]=-1.0f;  //this means to ignore this weight
          }
        } else {
          //we are within the head...there are a couple of things to clean up

          //first, is this a legit value?  It should be >= 0.0.  If it isn't, it was a
          //quantization problem.  let's clean it up.
          for (int Ielec=0; Ielec<n_elec; Ielec++) {
            if (weightFac[Ielec][Ix][Iy] < 0.0f) {
              weightFac[Ielec][Ix][Iy] = getClosestWeightFac(weightFac[Ielec], Ix, Iy);
            }
          }

          //next, is our pixel within an electrode.  If so, ensure it's weights
          //set the value to be the same as the electrode
          if (withinElectrode[Ix][Iy] > -1) {
            //we are!  set the weightFac to reflect this electrode only
            for (int Ielec=0; Ielec<n_elec; Ielec++) {
              weightFac[Ielec][Ix][Iy] = 0.0f; //ignore all other electrodes
              if (Ielec == withinElectrode[Ix][Iy]) {
                weightFac[Ielec][Ix][Iy] = 1.0f;  //become equal to this electrode
              }
            }
          } //close "if within electrode"
        } //close "if within head"
      } //close Iy
    } // close Ix
  } //close method

  //find the closest legitimate weightFac
  private float getClosestWeightFac(float weightFac[][], int Ix, int Iy) {
    int n_wide = weightFac.length;
    int n_tall = weightFac[0].length;
    float sum = 0.0f;
    int n_sum = 0;
    float new_weightFac=-1.0f;


    int step = 1;
    int Ix_test, Iy_test;
    boolean done = false;
    boolean anyWithinBounds;
    while (!done) {
      anyWithinBounds = false;

      //search the perimeter at this distance
      sum = 0.0f;
      n_sum = 0;

      //along the top
      Iy_test = Iy + step;
      if ((Iy_test >= 0) && (Iy_test < n_tall)) {
        for (Ix_test=Ix-step; Ix_test<=Ix+step; Ix_test++) {
          if ((Ix_test >=0) && (Ix_test < n_wide)) {
            anyWithinBounds=true;
            if (weightFac[Ix_test][Iy_test] >= 0.0f) {
              sum += weightFac[Ix_test][Iy_test];
              n_sum++;
            }
          }
        }
      }

      //along the right
      Ix_test = Ix + step;
      if ((Ix_test >= 0) && (Ix_test < n_wide)) {
        for (Iy_test=Iy-step; Iy_test<=Iy+step; Iy_test++) {
          if ((Iy_test >=0) && (Iy_test < n_tall)) {
            anyWithinBounds=true;
            if (weightFac[Ix_test][Iy_test] >= 0.0f) {
              sum += weightFac[Ix_test][Iy_test];
              n_sum++;
            }
          }
        }
      }
      //along the bottom
      Iy_test = Iy - step;
      if ((Iy_test >= 0) && (Iy_test < n_tall)) {
        for (Ix_test=Ix-step; Ix_test<=Ix+step; Ix_test++) {
          if ((Ix_test >=0) && (Ix_test < n_wide)) {
            anyWithinBounds=true;
            if (weightFac[Ix_test][Iy_test] >= 0.0f) {
              sum += weightFac[Ix_test][Iy_test];
              n_sum++;
            }
          }
        }
      }

      //along the left
      Ix_test = Ix - step;
      if ((Ix_test >= 0) && (Ix_test < n_wide)) {
        for (Iy_test=Iy-step; Iy_test<=Iy+step; Iy_test++) {
          if ((Iy_test >=0) && (Iy_test < n_tall)) {
            anyWithinBounds=true;
            if (weightFac[Ix_test][Iy_test] >= 0.0f) {
              sum += weightFac[Ix_test][Iy_test];
              n_sum++;
            }
          }
        }
      }

      if (n_sum > 0) {
        //some good pixels were found, so we have our answer
        new_weightFac = sum / n_sum; //complete the averaging process
        done = true; //we're done
      } else {
        //we did not find any good pixels.  Step outward one more pixel and repeat the search
        step++;  //step outwward
        if (anyWithinBounds) {  //did the last iteration have some pixels that were at least within the domain
          //some pixels were within the domain, so we have space to try again
          done = false;
        } else {
          //no pixels were within the domain.  We're out of space.  We're done.
          done = true;
        }
      }
    }
    return new_weightFac; //good or bad, return our new value
  }

  private void computeWeightFactorsGivenOneElectrode_iterative(int toPixels[][][][], int toElectrodes[][][], int Ielec, float pixelVal[][][]) {
    //Approach: pretend that one electrode is set to 1.0 and that all other electrodes are set to 0.0.
    //Assume all of the pixels start at zero.  Then, begin the simulation as if it were a transient
    //solution where energy is coming in from the connections.  Any excess energy will accumulate
    //and cause the local pixel's value to increase.  Iterate until the pixel values stabalize.

    int n_wide = toPixels.length;
    int n_tall = toPixels[0].length;
    int n_dir = toPixels[0][0].length;
    float prevVal[][] = new float[n_wide][n_tall];
    float total, dVal;
    int Ix_targ, Iy_targ;
    float min_val=0.0f, max_val=0.0f;
    boolean anyConnections = false;
    int pixel_step = 1;

    //initialize all pixels to zero
    //for (int Ix=0; Ix<n_wide;Ix++) { for (int Iy=0; Iy<n_tall;Iy++) { pixelVal[Ielec][Ix][Iy]=0.0f; }; };

    //define the iteration limits
    int lim_iter_count = 2000;  //set to something big enough to get the job done, but not so big that it could take forever
    float dVal_threshold = 0.00001f;  //set to something arbitrarily small
    float change_fac = 0.2f; //must be small enough to keep this iterative solution stable.  Goes unstable above 0.25

    //begin iteration
    int iter_count = 0;
    float max_dVal = 10.0f*dVal_threshold;  //initilize to large value to ensure that it starts
    while ((iter_count < lim_iter_count) && (max_dVal > dVal_threshold)) {
      //increment the counter
      iter_count++;

      //reset our test value to a large value
      max_dVal = 0.0f;

      //reset other values that I'm using for debugging
      min_val = 1000.0f; //init to a big val
      max_val = -1000.f; //init to a small val

      //copy current values
      for (int Ix=0; Ix<n_wide; Ix++) {
        for (int Iy=0; Iy<n_tall; Iy++) {
          prevVal[Ix][Iy]=pixelVal[Ielec][Ix][Iy];
        };
      };

      //compute the new pixel values
      for (int Ix=0; Ix<n_wide; Ix+=pixel_step) {
        for (int Iy=0; Iy<n_tall; Iy+=pixel_step) {
          //reset variables related to this one pixel
          total=0.0f;
          anyConnections = false;

          for (int Idir=0; Idir<n_dir; Idir++) {
            //do we connect to a real pixel?
            if (toPixels[Ix][Iy][Idir][0] > -1) {
              Ix_targ = toPixels[Ix][Iy][Idir][0];  //x index of target pixel
              Iy_targ = toPixels[Ix][Iy][Idir][1];  //y index of target pixel
              total += (prevVal[Ix_targ][Iy_targ]-prevVal[Ix][Iy]);  //difference relative to target pixel
              anyConnections = true;
            }
            //do we connect to an electrode?
            if (toElectrodes[Ix][Iy][Idir] > -1) {
              //do we connect to the electrode that we're stimulating
              if (toElectrodes[Ix][Iy][Idir] == Ielec) {
                //yes, this is the active high one
                total += (1.0f-prevVal[Ix][Iy]);  //difference relative to HIGH electrode
              } else {
                //no, this is a low one
                total += (0.0f-prevVal[Ix][Iy]);  //difference relative to the LOW electrode
              }
              anyConnections = true;
            }
          }

          //compute the new pixel value
          //if (numConnections[Ix][Iy] > 0) {
          if (anyConnections) {

            //dVal = change_fac * (total - float(numConnections[Ix][Iy])*prevVal[Ix][Iy]);
            dVal = change_fac * total;
            pixelVal[Ielec][Ix][Iy] = prevVal[Ix][Iy] + dVal;

            //is this our worst change in value?
            max_dVal = max(max_dVal, abs(dVal));

            //update our other debugging values, too
            min_val = min(min_val, pixelVal[Ielec][Ix][Iy]);
            max_val = max(max_val, pixelVal[Ielec][Ix][Iy]);
          } else {
            pixelVal[Ielec][Ix][Iy] = -1.0f; //means that there are no connections
          }
        }
      }
      //println("headPlot: computeWeightFactor: Ielec " + Ielec + ", iter = " + iter_count + ", max_dVal = " + max_dVal);
    }
    //println("headPlot: computeWeightFactor: Ielec " + Ielec + ", solution complete with " + iter_count + " iterations. min and max vals = " + min_val + ", " + max_val);
    if (iter_count >= lim_iter_count) println("headPlot: computeWeightFactor: Ielec " + Ielec + ", solution complete with " + iter_count + " iterations. max_dVal = " + max_dVal);
  } //end of method



  //  private void countConnections(int toPixels[][][][],int toElectrodes[][][], int numConnections[][]) {
  //    int n_wide = toPixels.length;
  //    int n_tall = toPixels[0].length;
  //    int n_dir = toPixels[0][0].length;
  //
  //    //loop over each pixel
  //    for (int Ix=0; Ix<n_wide;Ix++) {
  //      for (int Iy=0; Iy<n_tall;Iy++) {
  //
  //        //initialize
  //        numConnections[Ix][Iy]=0;
  //
  //        //loop through the four directions
  //        for (int Idir=0;Idir<n_dir;Idir++) {
  //          //is it a connection to another pixel (anything > -1 is a connection)
  //          if (toPixels[Ix][Iy][Idir][0] > -1) numConnections[Ix][Iy]++;
  //
  //          //is it a connection to an electrode?
  //          if (toElectrodes[Ix][Iy][Idir] > -1) numConnections[Ix][Iy]++;
  //        }
  //      }
  //    }
  //  }

  private void makeAllTheConnections(boolean withinHead[][], int withinElectrode[][], int toPixels[][][][], int toElectrodes[][][]) {

    int n_wide = toPixels.length;
    int n_tall = toPixels[0].length;
    int n_elec = electrode_xy.length;
    int curPixel, Ipix, Ielec;
    int n_pixels = n_wide * n_tall;
    int Ix_try, Iy_try;


    //loop over every pixel in the image
    for (int Iy=0; Iy < n_tall; Iy++) {
      for (int Ix=0; Ix < n_wide; Ix++) {

        //loop over the four connections: left, right, up, down
        for (int Idirection = 0; Idirection < 4; Idirection++) {

          Ix_try = -1;
          Iy_try=-1; //nonsense values
          switch (Idirection) {
          case 0:
            Ix_try = Ix-1;
            Iy_try = Iy; //left
            break;
          case 1:
            Ix_try = Ix+1;
            Iy_try = Iy; //right
            break;
          case 2:
            Ix_try = Ix;
            Iy_try = Iy-1; //up
            break;
          case 3:
            Ix_try = Ix;
            Iy_try = Iy+1; //down
            break;
          }

          //initalize to no connection
          toPixels[Ix][Iy][Idirection][0] = -1;
          toPixels[Ix][Iy][Idirection][1] = -1;
          toElectrodes[Ix][Iy][Idirection] = -1;

          //does the target pixel exist
          if ((Ix_try >= 0) && (Ix_try < n_wide)  && (Iy_try >= 0) && (Iy_try < n_tall)) {
            //is the target pixel an electrode
            if (withinElectrode[Ix_try][Iy_try] >= 0) {
              //the target pixel is within an electrode
              toElectrodes[Ix][Iy][Idirection] = withinElectrode[Ix_try][Iy_try];
            } else {
              //the target pixel is not within an electrode.  is it within the head?
              if (withinHead[Ix_try][Iy_try]) {
                toPixels[Ix][Iy][Idirection][0] = Ix_try; //save the address of the target pixel
                toPixels[Ix][Iy][Idirection][1] = Iy_try; //save the address of the target pixel
              }
            }
          }
        } //end loop over direction of the target pixel
      } //end loop over Ix
    } //end loop over Iy
  } // end of method

  private void whereAreThePixels(int pixelAddress[][][], boolean[][] withinHead, int[][] withinElectrode) {
    int n_wide = pixelAddress.length;
    int n_tall = pixelAddress[0].length;
    int n_elec = electrode_xy.length;
    int pixel_x, pixel_y;
    int withinElecInd=-1;
    float dist;
    float elec_radius = 0.5f*elec_diam;

    for (int Iy=0; Iy < n_tall; Iy++) {
      //pixel_y = image_y + Iy;
      for (int Ix = 0; Ix < n_wide; Ix++) {
        //pixel_x = image_x + Ix;

        pixel_x = pixelAddress[Ix][Iy][0]+image_x;
        pixel_y = pixelAddress[Ix][Iy][1]+image_y;

        //is it within the head
        withinHead[Ix][Iy] = isPixelInsideHead(pixel_x, pixel_y);

        //compute distances of this pixel to each electrode
        withinElecInd = -1;    //reset for this pixel
        for (int Ielec=0; Ielec < n_elec; Ielec++) {
          //compute distance
          dist = max(1.0f, calcDistance(pixel_x, pixel_y, electrode_xy[Ielec][0], electrode_xy[Ielec][1]));
          if (dist < elec_radius) withinElecInd = Ielec;
        }
        withinElectrode[Ix][Iy] = withinElecInd;  //-1 means not inside an electrode
      } //close Ix loop
    } //close Iy loop

    //ensure that each electrode is at at least one pixel
    for (int Ielec=0; Ielec<n_elec; Ielec++) {
      //find closest pixel
      float min_dist = 1.0e10f;  //some huge number
      int best_Ix=0, best_Iy=0;
      for (int Iy=0; Iy < n_tall; Iy++) {
        //pixel_y = image_y + Iy;
        for (int Ix = 0; Ix < n_wide; Ix++) {
          //pixel_x = image_x + Ix;

          pixel_x = pixelAddress[Ix][Iy][0]+image_x;
          pixel_y = pixelAddress[Ix][Iy][1]+image_y;

          dist = calcDistance(pixel_x, pixel_y, electrode_xy[Ielec][0], electrode_xy[Ielec][1]);
          ;

          if (dist < min_dist) {
            min_dist = dist;
            best_Ix = Ix;
            best_Iy = Iy;
          }
        } //close Iy loop
      } //close Ix loop

      //define this closest point to be within the electrode
      withinElectrode[best_Ix][best_Iy] = Ielec;
    } //close Ielec loop
  } //close method


  //step through pixel-by-pixel to update the image
  private void updateHeadImage() {
    for (int Iy=0; Iy < headImage.height; Iy++) {
      for (int Ix = 0; Ix < headImage.width; Ix++) {
        //is this pixel inside the head?
        if (electrode_color_weightFac[0][Ix][Iy] >= 0.0f) { //zero and positive values are inside the head
          //it is inside the head.  set the color based on the electrodes
          headImage.set(Ix, Iy, calcPixelColor(Ix, Iy));
        } else {  //negative values are outside of the head
          //pixel is outside the head.  set to black.
          headImage.set(Ix, Iy, color(0, 0, 0, 0));
        }
      }
    }
  }

  private void convertVoltagesToHeadImage() {
    for (int Iy=0; Iy < headImage.height; Iy++) {
      for (int Ix = 0; Ix < headImage.width; Ix++) {
        //is this pixel inside the head?
        if (electrode_color_weightFac[0][Ix][Iy] >= 0.0f) { //zero and positive values are inside the head
          //it is inside the head.  set the color based on the electrodes
          headImage.set(Ix, Iy, calcPixelColor(headVoltage[Ix][Iy]));
        } else {  //negative values are outside of the head
          //pixel is outside the head.  set to black.
          headImage.set(Ix, Iy, color(0, 0, 0, 0));
        }
      }
    }
  }


  private void updateHeadVoltages() {
    for (int Iy=0; Iy < headImage.height; Iy++) {
      for (int Ix = 0; Ix < headImage.width; Ix++) {
        //is this pixel inside the head?
        if (electrode_color_weightFac[0][Ix][Iy] >= 0.0f) { //zero and positive values are inside the head
          //it is inside the head.  set the voltage based on the electrodes
          headVoltage[Ix][Iy] = calcPixelVoltage(Ix, Iy, headVoltage[Ix][Iy]);
        } else {  //negative values are outside of the head
          //pixel is outside the head.
          headVoltage[Ix][Iy] = -1.0f;
        }
      }
    }
  }

  int count_call=0;
  private float calcPixelVoltage(int pixel_Ix, int pixel_Iy, float prev_val) {
    float weight, elec_volt;
    int n_elec = electrode_xy.length;
    float voltage = 0.0f;
    float low = intense_min_uV;
    float high = intense_max_uV;

    for (int Ielec=0; Ielec<n_elec; Ielec++) {
      weight = electrode_color_weightFac[Ielec][pixel_Ix][pixel_Iy];
      elec_volt = max(low, min(intensity_data_uV[Ielec], high));

      if (use_polarity) elec_volt = elec_volt*polarity_data[Ielec];

      if (is_railed[Ielec].is_railed) elec_volt = assumed_railed_voltage_uV;
      voltage += weight*elec_volt;
    }

    //smooth in time
    if (smooth_fac > 0.0f) voltage = smooth_fac*prev_val + (1.0f-smooth_fac)*voltage;

    return voltage;
  }


  private int calcPixelColor(float pixel_volt_uV) {
    // float new_rgb[] = {255.0, 0.0, 0.0}; //init to red
    //224, 56, 45
    float new_rgb[] = {224.0f, 56.0f, 45.0f}; //init to red
    // float new_rgb[] = {0.0, 255.0, 0.0}; //init to red
    //54, 87, 158
    if (pixel_volt_uV < 0.0f) {
      //init to blue instead
      new_rgb[0]=54.0f;
      new_rgb[1]=87.0f;
      new_rgb[2]=158.0f;
      // new_rgb[0]=0.0;
      // new_rgb[1]=0.0;
      // new_rgb[2]=255.0;
    }
    float val;


    float intensity = constrain(abs(pixel_volt_uV), intense_min_uV, intense_max_uV);
    if (plot_color_as_log) {
      intensity = map(log10(intensity),
        log10_intense_min_uV,
        log10_intense_max_uV,
        0.0f, 1.0f);
    } else {
      intensity = map(intensity,
        intense_min_uV,
        intense_max_uV,
        0.0f, 1.0f);
    }

    //make the intensity fade NOT from black->color, but from white->color
    for (int i=0; i < 3; i++) {
      val = ((float)new_rgb[i]) / 255.f;
      new_rgb[i] = ((val + (1.0f - val)*(1.0f-intensity))*255.f); //adds in white at low intensity.  no white at high intensity
      new_rgb[i] = constrain(new_rgb[i], 0.0f, 255.0f);
    }

    //quantize the color to make contour-style plot?
    if (true) quantizeColor(new_rgb);

    return color(PApplet.parseInt(new_rgb[0]), PApplet.parseInt(new_rgb[1]), PApplet.parseInt(new_rgb[2]), 255);
  }

  private void quantizeColor(float new_rgb[]) {
    int n_colors = 12;
    int ticks_per_color = 256 / (n_colors+1);
    for (int Irgb=0; Irgb<3; Irgb++) new_rgb[Irgb] = min(255.0f, PApplet.parseFloat(PApplet.parseInt(new_rgb[Irgb]/ticks_per_color))*ticks_per_color);
  }


  //compute the color of the pixel given the location
  private int calcPixelColor(int pixel_Ix, int pixel_Iy) {
    float weight;

    //compute the weighted average using the precomputed factors
    float new_rgb[] = {0.0f, 0.0f, 0.0f}; //init to zeros
    for (int Ielec=0; Ielec < electrode_xy.length; Ielec++) {
      //int Ielec = 0;
      weight = electrode_color_weightFac[Ielec][pixel_Ix][pixel_Iy];
      for (int Irgb=0; Irgb<3; Irgb++) {
        new_rgb[Irgb] += weight*electrode_rgb[Irgb][Ielec];
      }
    }

    //quantize the color to make contour-style plot?
    if (true) quantizeColor(new_rgb);

    return color(PApplet.parseInt(new_rgb[0]), PApplet.parseInt(new_rgb[1]), PApplet.parseInt(new_rgb[2]), 255);
  }

  private float calcDistance(int x, int y, float ref_x, float ref_y) {
    float dx = PApplet.parseFloat(x) - ref_x;
    float dy = PApplet.parseFloat(y) - ref_y;
    return sqrt(dx*dx + dy*dy);
  }

  //compute color for the electrode value
  private void updateElectrodeColors() {
    int rgb[] = new int[]{255, 0, 0}; //color for the electrode when fully light
    float intensity;
    float val;
    int new_rgb[] = new int[3];
    float low = intense_min_uV;
    float high = intense_max_uV;
    float log_low = log10_intense_min_uV;
    float log_high = log10_intense_max_uV;
    for (int Ielec=0; Ielec < electrode_xy.length; Ielec++) {
      intensity = constrain(intensity_data_uV[Ielec], low, high);
      if (plot_color_as_log) {
        intensity = map(log10(intensity), log_low, log_high, 0.0f, 1.0f);
      } else {
        intensity = map(intensity, low, high, 0.0f, 1.0f);
      }

      //make the intensity fade NOT from black->color, but from white->color
      for (int i=0; i < 3; i++) {
        val = ((float)rgb[i]) / 255.f;
        new_rgb[i] = (int)((val + (1.0f - val)*(1.0f-intensity))*255.f); //adds in white at low intensity.  no white at high intensity
        new_rgb[i] = constrain(new_rgb[i], 0, 255);
      }

      //change color to dark RED if railed
      if (is_railed[Ielec].is_railed)  new_rgb = new int[]{127, 0, 0};

      //set the electrode color
      electrode_rgb[0][Ielec] = new_rgb[0];
      electrode_rgb[1][Ielec] = new_rgb[1];
      electrode_rgb[2][Ielec] = new_rgb[2];
    }
  }


  public boolean isPixelInsideHead(int pixel_x, int pixel_y) {
    int dx = pixel_x - circ_x;
    int dy = pixel_y - circ_y;
    float r = sqrt(PApplet.parseFloat(dx*dx) + PApplet.parseFloat(dy*dy));
    if (r <= 0.5f*circ_diam) {
      return true;
    } else {
      return false;
    }
  }

  public void update() {
    //do this when new data is available

    //update electrode colors
    updateElectrodeColors();

    if (false) {
      //update the head image
      if (drawHeadAsContours) updateHeadImage();
    } else {
      //update head voltages
      updateHeadVoltages();
      convertVoltagesToHeadImage();
    }
  }

  public void draw() {

    pushStyle();
    smooth();
    //draw head parts
    fill(255, 255, 255);
    stroke(125, 125, 125);
    triangle(nose_x[0], nose_y[0], nose_x[1], nose_y[1], nose_x[2], nose_y[2]);  //nose
    ellipse(earL_x, earL_y, ear_width, ear_height); //little circle for the ear
    ellipse(earR_x, earR_y, ear_width, ear_height); //little circle for the ear

    //draw head itself
    fill(255, 255, 255, 255);  //fill in a white head
    strokeWeight(1);
    ellipse(circ_x, circ_y, circ_diam, circ_diam); //big circle for the head
    if (drawHeadAsContours) {
      //add the contnours
      image(headImage, image_x, image_y);
      noFill(); //overlay a circle as an outline, but no fill
      strokeWeight(1);
      ellipse(circ_x, circ_y, circ_diam, circ_diam); //big circle for the head
    }

    //draw electrodes on the head
    strokeWeight(1);
    for (int Ielec=0; Ielec < electrode_xy.length; Ielec++) {
      if (drawHeadAsContours) {
        noFill(); //make transparent to allow color to come through from below
      } else {
        fill(electrode_rgb[0][Ielec], electrode_rgb[1][Ielec], electrode_rgb[2][Ielec]);
      }
      ellipse(electrode_xy[Ielec][0], electrode_xy[Ielec][1], elec_diam, elec_diam); //big circle for the head
    }

    //add labels to electrodes
    fill(0, 0, 0);
    textFont(font);
    textAlign(CENTER, CENTER);
    for (int i=0; i < electrode_xy.length; i++) {
      //text(Integer.toString(i),electrode_xy[i][0], electrode_xy[i][1]);
      text(i+1, electrode_xy[i][0], electrode_xy[i][1]);
    }
    text("R", ref_electrode_xy[0], ref_electrode_xy[1]);

    popStyle();
  } //end of draw method
};

////////////////////////////////////////////////////
//
//    W_template.pde (ie "Widget Template")
//
//    This is a Template Widget, intended to be used as a starting point for OpenBCI Community members that want to develop their own custom widgets!
//    Good luck! If you embark on this journey, please let us know. Your contributions are valuable to everyone!
//
//    Created by: Conor Russomanno, November 2016
//
///////////////////////////////////////////////////,

class W_networking extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  Button widgetTemplateButton;
  int protocolMode = 0;

  W_networking(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    addDropdown("Protocol", "Drop 1", Arrays.asList("OSC", "UDC", "LSL", "Serial"), protocolMode);
    // addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    // addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

    widgetTemplateButton = new Button (x + w/2, y + h/2, 200, navHeight, "Design Your Own Widget!", 12);
    widgetTemplateButton.setFont(p4, 14);
    widgetTemplateButton.setURL("http://docs.openbci.com/OpenBCI%20Software/");

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

    if(protocolMode == 0){
      fill(255,0,0);
    } else if (protocolMode == 1){
      fill(0,255,0);
    } else if (protocolMode == 2){
      fill(0,0,255);
    } else if (protocolMode == 3){
      fill(0,255,255);
    }

    rect(x, y, w, h);

    widgetTemplateButton.draw();

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    widgetTemplateButton.setPos(x + w/2 - widgetTemplateButton.but_dx/2, y + h/2 - widgetTemplateButton.but_dy/2);


  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(widgetTemplateButton.isMouseHere()){
      widgetTemplateButton.setIsActive(true);
    }

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(widgetTemplateButton.isActive && widgetTemplateButton.isMouseHere()){
      widgetTemplateButton.goToURL();
    }
    widgetTemplateButton.setIsActive(false);

  }

  //add custom functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget

  }

};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void Protocol(int n){
  println("Item " + (n+1) + " selected from Dropdown 1");
  // if(n==0){
  //   protcolMode = 0;
  // } else if(n==1){
  //   protcolMode = 1;
  // } else if(n==2){
  //   protcolMode = 2;
  // } else if(n==3){
  //   protcolMode = 3;
  // }
  w_networking.protocolMode = n;

  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//    Widget
//      the idea here is that the widget class takes care of all of the responsiveness/structural stuff in the bg so that it is very easy to create a new custom widget to add to the GUI
//      the "Widgets" will be able to be mapped to the various containers of the GUI
//      created by Conor Russomanno ... 11/17/2016
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


class Widget{

  PApplet pApplet;

  int x0, y0, w0, h0; //true x,y,w,h of container
  int x, y, w, h; //adjusted x,y,w,h of white space (blank rectangle) under the nav...

  int currentContainer; //this determines where the widget is located ... based on the x/y/w/h of the parent container

  boolean isActive = false;
  boolean dropdownsShouldBeClosed = false;

  ArrayList<NavBarDropdown> dropdowns;
  ControlP5 cp5_widget;
  String widgetTitle = "No Title Set";
  Button widgetSelector;

  //some variables for the dropdowns
  int navH = 22;
  int widgetSelectorWidth = 160;
  int dropdownWidth = 64;

  CColor dropdownColors = new CColor(); //this is a global CColor that determines the style of all widget dropdowns ... this should go in WidgetManager.pde

  Widget(PApplet _parent){
    pApplet = _parent;
    cp5_widget = new ControlP5(pApplet);
    dropdowns = new ArrayList<NavBarDropdown>();
    //setup dropdown menus

    currentContainer = 5; //central container by default
    mapToCurrentContainer();

  }

  public void update(){

    updateDropdowns();

  }

  public void draw(){
    pushStyle();

    fill(255);
    rect(x,y-1,w,h+1); //draw white widget background

    //draw nav bars and button bars
    fill(150, 150, 150);
    rect(x0, y0, w0, navH); //top bar
    fill(200, 200, 200);
    rect(x0, y0+navH, w0, navH); //button bar

    // fill(255);
    // rect(x+2, y+2, navH-4, navH-4);
    // fill(bgColor, 100);
    // rect(x+4, y+4, (navH-10)/2, (navH-10)/2);
    // rect(x+4, y+((navH-10)/2)+5, (navH-10)/2, (navH-10)/2);
    // rect(x+((navH-10)/2)+5, y+4, (navH-10)/2, (navH-10)/2);
    // rect(x+((navH-10)/2)+5, y+((navH-10)/2)+5, (navH-10)/2, (navH-10 )/2);
    //
    // fill(bgColor);
    // textAlign(LEFT, CENTER);
    // textFont(h2);
    // textSize(16);
    // text(widgetTitle, x+navH+2, y+navH/2 - 2); //title of widget -- left

    // drawDropdowns(); //moved to WidgetManager, so that dropdowns draw on top of widget content

    popStyle();
  }

  public void addDropdown(String _id, String _title, List _items, int _defaultItem){
    NavBarDropdown dropdownToAdd = new NavBarDropdown(_id, _title, _items, _defaultItem);
    dropdowns.add(dropdownToAdd);
  }

  public void setupWidgetSelectorDropdown(ArrayList<String> _widgetOptions){
    cp5_widget.setAutoDraw(false); //this prevents the cp5 object from drawing automatically (if it is set to true it will be drawn last, on top of all other GUI stuff... not good)
    // cp5_widget.setFont(h2, 16);
    // cp5_widget.getCaptionLabel().toUpperCase(false);
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //      SETUP the widgetSelector dropdown
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    dropdownColors.setActive((int)color(150, 170, 200)); //bg color of box when pressed
    dropdownColors.setForeground((int)color(125)); //when hovering over any box (primary or dropdown)
    dropdownColors.setBackground((int)color(255)); //bg color of boxes (including primary)
    dropdownColors.setCaptionLabel((int)color(1, 18, 41)); //color of text in primary box
    // dropdownColors.setValueLabel((int)color(1, 18, 41)); //color of text in all dropdown boxes
    dropdownColors.setValueLabel((int)color(100)); //color of text in all dropdown boxes


    print("wm.widgetOptions.size() = ");
    println(_widgetOptions.size());

    cp5_widget.setColor(dropdownColors);
    cp5_widget.addScrollableList("WidgetSelector")
      .setPosition(x0+2, y0+2) //upper left corner
      // .setFont(h2)
      .setOpen(false)
      .setColor(dropdownColors)
      .setSize(widgetSelectorWidth, (_widgetOptions.size()+1)*(navH-4) )// + maxFreqList.size())
      // .setScrollSensitivity(0.0)
      .setBarHeight(navH-4) //height of top/primary bar
      .setItemHeight(navH-4) //height of all item/dropdown bars
      .addItems(_widgetOptions) // used to be .addItems(maxFreqList)
      ;
    cp5_widget.getController("WidgetSelector")
      .getCaptionLabel() //the caption label is the text object in the primary bar
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText(widgetTitle)
      .setFont(h4)
      .setSize(14)
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(4)
      ;
    cp5_widget.getController("WidgetSelector")
      .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText(widgetTitle)
      .setFont(h5)
      .setSize(12) //set the font size of the item bars to 14pt
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(3) //4-pixel vertical offset to center text
      ;
  }

  public void setupNavDropdowns(){

    cp5_widget.setAutoDraw(false); //this prevents the cp5 object from drawing automatically (if it is set to true it will be drawn last, on top of all other GUI stuff... not good)
    // cp5_widget.setFont(h3, 12);

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //      SETUP all NavBarDropdowns
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    dropdownColors.setActive((int)color(150, 170, 200)); //bg color of box when pressed
    dropdownColors.setForeground((int)color(177, 184, 193)); //when hovering over any box (primary or dropdown)
    // dropdownColors.setForeground((int)color(125)); //when hovering over any box (primary or dropdown)
    dropdownColors.setBackground((int)color(255)); //bg color of boxes (including primary)
    dropdownColors.setCaptionLabel((int)color(1, 18, 41)); //color of text in primary box
    // dropdownColors.setValueLabel((int)color(1, 18, 41)); //color of text in all dropdown boxes
    dropdownColors.setValueLabel((int)color(100)); //color of text in all dropdown boxes

    cp5_widget.setColor(dropdownColors);
    // println("Setting up dropdowns...");
    for(int i = 0; i < dropdowns.size(); i++){
      int dropdownPos = dropdowns.size() - i;
      // println("dropdowns.get(i).id = " + dropdowns.get(i).id);
      cp5_widget.addScrollableList(dropdowns.get(i).id)
        .setPosition(x0+w0-(dropdownWidth*(dropdownPos))-(2*(dropdownPos)), y0 + navH + 2) //float right
        .setFont(h5)
        .setOpen(false)
        .setColor(dropdownColors)
        .setSize(dropdownWidth, (dropdowns.get(i).items.size()+1)*(navH-4) )// + maxFreqList.size())
        .setBarHeight(navH-4)
        .setItemHeight(navH-4)
        .addItems(dropdowns.get(i).items) // used to be .addItems(maxFreqList)
        ;
      cp5_widget.getController(dropdowns.get(i).id)
        .getCaptionLabel()
        .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
        .setText(dropdowns.get(i).returnDefaultAsString())
        .setSize(12)
        .getStyle()
        .setPaddingTop(4)
        ;
      cp5_widget.getController(dropdowns.get(i).id)
        .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
        .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
        .setText(widgetTitle)
        .setSize(12) //set the font size of the item bars to 14pt
        .getStyle() //need to grab style before affecting the paddingTop
        .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
    }
  }
  public void updateDropdowns(){
    //if a dropdown is open and mouseX/mouseY is outside of dropdown, then close it
    // println("dropdowns.size() = " + dropdowns.size());
    if(cp5_widget.get(ScrollableList.class, "WidgetSelector").isOpen()){
      if(!cp5_widget.getController("WidgetSelector").isMouseOver()){
        // println("2");
        cp5_widget.get(ScrollableList.class, "WidgetSelector").close();
      }
    }

    for(int i = 0; i < dropdowns.size(); i++){
      // println("i = " + i);
      if(cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).isOpen()){
        // println("1");
        if(!cp5_widget.getController(dropdowns.get(i).id).isMouseOver()){
          // println("2");
          cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).close();
        }
      }
    }

    //onHover ... open ... no need to click
    if(dropdownsShouldBeClosed){ //this if takes care of the scenario where you select the same widget that is active...
      dropdownsShouldBeClosed = false;
    } else{
      if(!cp5_widget.get(ScrollableList.class, "WidgetSelector").isOpen()){
        if(cp5_widget.getController("WidgetSelector").isMouseOver()){
          // println("2");
          cp5_widget.get(ScrollableList.class, "WidgetSelector").open();
        }
      }

      for(int i = 0; i < dropdowns.size(); i++){
        // println("i = " + i);
        if(!cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).isOpen()){
          // println("1");
          if(cp5_widget.getController(dropdowns.get(i).id).isMouseOver()){
            // println("2");
            cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).open();
          }
        }
      }
    }

    //make sure that the widgetSelector CaptionLabel always corresponds to its widget
    cp5_widget.getController("WidgetSelector")
      .getCaptionLabel()
      .setText(widgetTitle)
      ;

  }

  public void drawDropdowns(){

    //draw dropdown titles
    pushStyle();

    noStroke();
    textFont(h5);
    textSize(12);
    textAlign(CENTER, BOTTOM);
    fill(bgColor);
    for(int i = 0; i < dropdowns.size(); i++){
      int dropdownPos = dropdowns.size() - i;
      // text(dropdowns.get(i).title, x+w-(dropdownWidth*(dropdownPos+1))-(2*(dropdownPos+1))+dropdownWidth/2, y+(navH-2));
      text(dropdowns.get(i).title, x0+w0-(dropdownWidth*(dropdownPos))-(2*(dropdownPos+1))+dropdownWidth/2, y0+(navH-2));
    }

    //draw background/stroke of widgetSelector dropdown
    fill(150);
    rect(cp5_widget.getController("WidgetSelector").getPosition()[0]-1, cp5_widget.getController("WidgetSelector").getPosition()[1]-1, widgetSelectorWidth+2, cp5_widget.get(ScrollableList.class, "WidgetSelector").getHeight()+2);

    //draw backgrounds to dropdown scrollableLists ... unfortunately ControlP5 doesn't have this by default, so we have to hack it to make it look nice...
    fill(200);
    for(int i = 0; i < dropdowns.size(); i++){
      rect(cp5_widget.getController(dropdowns.get(i).id).getPosition()[0] - 1, cp5_widget.getController(dropdowns.get(i).id).getPosition()[1] - 1, dropdownWidth + 2, cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).getHeight()+2);
    }

    textAlign(RIGHT, TOP);
    cp5_widget.draw(); //this draws all cp5 elements... in this case, the scrollable lists that populate our dropdowns<>

    popStyle();
  }

  public void screenResized(){
    mapToCurrentContainer();
  }

  public void mousePressed(){

  }

  public void mouseReleased(){

  }

  public void setTitle(String _widgetTitle){
    widgetTitle = _widgetTitle;
  }

  public void setContainer(int _currentContainer){
    currentContainer = _currentContainer;
    mapToCurrentContainer();
    screenResized();

  }

  public void mapToCurrentContainer(){
    x0 = (int)container[currentContainer].x;
    y0 = (int)container[currentContainer].y;
    w0 = (int)container[currentContainer].w;
    h0 = (int)container[currentContainer].h;

    x = x0;
    y = y0 + navH*2;
    w = w0;
    h = h0 - navH*2;

    cp5_widget.setGraphics(pApplet, 0, 0);

    // println("testing... 1. 2. 3....");
    try {
      cp5_widget.getController("WidgetSelector")
        .setPosition(x0+2, y0+2) //upper left corner
        ;
    }
    catch (Exception e) {
      println(e.getMessage());
      println("widgetOptions List not built yet...");
    }

    for(int i = 0; i < dropdowns.size(); i++){
      int dropdownPos = dropdowns.size() - i;
      cp5_widget.getController(dropdowns.get(i).id)
        //.setPosition(w-(dropdownWidth*dropdownPos)-(2*(dropdownPos+1)), navHeight+(y+2)) // float left
        .setPosition(x0+w0-(dropdownWidth*(dropdownPos))-(2*(dropdownPos)), navH +(y0+2)) //float right
        //.setSize(dropdownWidth, (maxFreqList.size()+1)*(navBarHeight-4))
        ;
    }
  }

  public boolean isMouseHere(){
    if(isActive){
      if(mouseX >= x0 && mouseX <= x0 + w0 && mouseY >= y0 && mouseY <= y0 + h0){
        println("Your cursor is in " + widgetTitle);
        return true;
      } else{
        return false;
      }
    } else {
      return false;
    }
  }
};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//    NavBarDropdown is a single dropdown item in any instance of a Widget
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class NavBarDropdown{

  String id;
  String title;
  // String[] items;
  List<String> items;
  int defaultItem;

  NavBarDropdown(String _id, String _title, List _items, int _defaultItem){
    id = _id;
    title = _title;
    // int dropdownSize = _items.length;
    // items = new String[_items.length];
    items = _items;

    defaultItem = _defaultItem;
  }

  public void update(){

  }

  public void draw(){

  }

  public void screenResized(){

  }

  public void mousePressed(){

  }

  public void mouseReleased(){

  }

  public String returnDefaultAsString(){
    String _defaultItem = items.get(defaultItem);
    return _defaultItem;
  }

}

public void closeAllDropdowns(){
  //close all dropdowns
  for(int i = 0; i < wm.widgets.size(); i++){
    wm.widgets.get(i).dropdownsShouldBeClosed = true;
  }
}

public void WidgetSelector(int n){
  println("New widget [" + n + "] selected for container...");
  //find out if the widget you selected is already active
  boolean isSelectedWidgetActive = wm.widgets.get(n).isActive;

  //find out which widget & container you are currently in...
  int theContainer = -1;
  for(int i = 0; i < wm.widgets.size(); i++){
    if(wm.widgets.get(i).isMouseHere()){
      theContainer = wm.widgets.get(i).currentContainer; //keep track of current container (where mouse is...)
      if(isSelectedWidgetActive){ //if the selected widget was already active
        wm.widgets.get(i).setContainer(wm.widgets.get(n).currentContainer); //just switch the widget locations (ie swap containers)
      } else{
        wm.widgets.get(i).isActive = false;   //deactivate the current widget (if it is different than the one selected)
      }
    }
  }

  wm.widgets.get(n).isActive = true;//activate the new widget
  wm.widgets.get(n).setContainer(theContainer);//map it to the current container
  //set the text of the widgetSelector to the newly selected widget

  closeAllDropdowns();
}

int navHeight = 22;

//========================================================================================
//=================              ADD NEW WIDGETS HERE            =========================
//========================================================================================
/*
  Notes:
  - In this file all you have to do is MAKE YOUR WIDGET GLOBALLY, and then ADD YOUR WIDGET TO WIDGETS OF WIDGETMANAGER in the setupWidgets() function below
  - the order in which they are added will effect the order in which they appear in the GUI and in the WidgetSelector dropdown menu of each widget
  - use the WidgetTemplate.pde file as a starting point for creating new widgets (also check out W_timeSeries.pde, W_fft.pde, and W_headPlot.pde)
*/

// MAKE YOUR WIDGET GLOBALLY
W_timeSeries w_timeSeries;
W_fft w_fft;
W_headPlot w_headPlot;
W_accelerometer w_accelerometer;
W_networking w_networking;
W_ganglionImpedance w_ganglionImpedance;
W_template w_template1;
W_emg w_emg;
W_matrix w_matrix;
W_openBionics w_openbionics;

//ADD YOUR WIDGET TO WIDGETS OF WIDGETMANAGER
public void setupWidgets(PApplet _this, ArrayList<Widget> w){
  w_timeSeries = new W_timeSeries(_this);
  w_timeSeries.setTitle("Time Series");
  addWidget(w_timeSeries, w);
  
   w_matrix = new W_matrix(_this);
  w_matrix.setTitle("Darwin Visual Matrix");
  addWidget(w_matrix, w);

  w_template1 = new W_template(_this);
  w_template1.setTitle("Darwin Template");
  addWidget(w_template1, w);

  w_fft = new W_fft(_this);
  w_fft.setTitle("FFT Plot");
  addWidget(w_fft, w);

  //only instantiate this widget if you are using a Ganglion board for live streaming
  if(nchan == 4 && eegDataSource == DATASOURCE_GANGLION){
    w_ganglionImpedance = new W_ganglionImpedance(_this);
    w_ganglionImpedance.setTitle("Ganglion Signal");
    addWidget(w_ganglionImpedance, w);
  }

  w_headPlot = new W_headPlot(_this);
  w_headPlot.setTitle("Head Plot");
  addWidget(w_headPlot, w);

  w_accelerometer = new W_accelerometer(_this);
  w_accelerometer.setTitle("Accelerometer");
  addWidget(w_accelerometer, w);

  // w_networking = new W_networking(_this);
  // w_networking.setTitle("Networking");
  // addWidget(w_networking, w);

  w_emg = new W_emg(_this);
  w_emg.setTitle("EMG");
  addWidget(w_emg, w);
  
 

  // w_template2 = new W_template(_this);
  // w_template2.setTitle("Widget Template 2");
  // addWidget(w_template2, w);

  // w_openbionics = new W_OpenBionics(_this);
  // w_openbionics.setTitle("OpenBionics");
  // addWidget(w_openbionics,w);

  // w_template3 = new W_template(_this);
  // w_template3.setTitle("LSL Stream");
  // addWidget(w_template3, w);

}

//========================================================================================
//========================================================================================
//========================================================================================

WidgetManager wm;
boolean wmVisible = true;
CColor cp5_colors;

//Channel Colors -- Defaulted to matching the OpenBCI electrode ribbon cable
int[] channelColors = {
  color(129, 129, 129),
  color(124, 75, 141),
  color(54, 87, 158),
  color(49, 113, 89),
  color(221, 178, 13),
  color(253, 94, 52),
  color(224, 56, 45),
  color(162, 82, 49)
};


class WidgetManager{

  //this holds all of the widgets ... when creating/adding new widgets, we will add them to this ArrayList (below)
  ArrayList<Widget> widgets;
  ArrayList<String> widgetOptions; //List of Widget Titles, used to populate cp5 widgetSelector dropdown of all widgets

  //Variables for
  int currentContainerLayout; //this is the Layout structure for the main body of the GUI ... refer to [PUT_LINK_HERE] for layouts/numbers image
  ArrayList<Layout> layouts = new ArrayList<Layout>();  //this holds all of the different layouts ...

  private boolean visible = true;
  private boolean updating = true;

  WidgetManager(PApplet _this){
    widgets = new ArrayList<Widget>();
    widgetOptions = new ArrayList<String>();

    //DO NOT re-order the functions below
    setupLayouts();
    setupWidgets(_this, widgets);
    setupWidgetSelectorDropdowns();

    if(nchan == 4 && eegDataSource == DATASOURCE_GANGLION){
      currentContainerLayout = 1;
      setNewContainerLayout(currentContainerLayout); //sets and fills layout with widgets in order of widget index, to reorganize widget index, reorder the creation in setupWidgets()
    } else {
      currentContainerLayout = 4; //default layout ... tall container left and 2 shorter containers stacked on the right
      setNewContainerLayout(currentContainerLayout); //sets and fills layout with widgets in order of widget index, to reorganize widget index, reorder the creation in setupWidgets()
    }
  }
  public boolean isVisible() {
    return visible;
  }
  public boolean isUpdating() {
    return updating;
  }

  public void setVisible(boolean _visible) {
    visible = _visible;
  }
  public void setUpdating(boolean _updating) {
    updating = _updating;
  }
  public void setupWidgetSelectorDropdowns(){
      //create the widgetSelector dropdown of each widget
      println("widgets.size() = " + widgets.size());
      //create list of WidgetTitles.. we will use this to populate the dropdown (widget selector) of each widget
      for(int i = 0; i < widgets.size(); i++){
        widgetOptions.add(widgets.get(i).widgetTitle);
      }
      println("widgetOptions.size() = " + widgetOptions.size());
      for(int i = 0; i <widgetOptions.size(); i++){
        widgets.get(i).setupWidgetSelectorDropdown(widgetOptions);
        widgets.get(i).setupNavDropdowns();
      }
      println("widgetOptions:");
      println(widgetOptions);
  }

  public void update(){
    if(visible && updating){
      for(int i = 0; i < widgets.size(); i++){
        if(widgets.get(i).isActive){
          widgets.get(i).update();
          //if the widgets are not mapped to containers correctly, remap them..
          // if(widgets.get(i).x != container[widgets.get(i).currentContainer].x || widgets.get(i).y != container[widgets.get(i).currentContainer].y || widgets.get(i).w != container[widgets.get(i).currentContainer].w || widgets.get(i).h != container[widgets.get(i).currentContainer].h){
          if(widgets.get(i).x0 != (int)container[widgets.get(i).currentContainer].x || widgets.get(i).y0 != (int)container[widgets.get(i).currentContainer].y || widgets.get(i).w0 != (int)container[widgets.get(i).currentContainer].w || widgets.get(i).h0 != (int)container[widgets.get(i).currentContainer].h){
            screenResized();
            println("WidgetManager.pde: Remapping widgets to container layout...");
          }
        }
      }
    }
  }

  public void draw(){
    if(visible){
      for(int i = 0; i < widgets.size(); i++){
        if(widgets.get(i).isActive){
          pushStyle();
          widgets.get(i).draw();
          widgets.get(i).drawDropdowns();
          popStyle();
        }
      }
    }
  }

  public void screenResized(){
    for(int i = 0; i < widgets.size(); i++){
      widgets.get(i).screenResized();
    }
  }

  public void mousePressed(){
    for(int i = 0; i < widgets.size(); i++){
      if(widgets.get(i).isActive){
        widgets.get(i).mousePressed();
      }

    }
  }

  public void mouseReleased(){
    for(int i = 0; i < widgets.size(); i++){
      if(widgets.get(i).isActive){
        widgets.get(i).mouseReleased();
      }
    }
  }

  public void setupLayouts(){
    //refer to [PUT_LINK_HERE] for layouts/numbers image
    //note that the order you create/add these layouts matters... if you reorganize these, the LayoutSelector will be out of order
    layouts.add(new Layout(new int[]{5})); //layout 1
    layouts.add(new Layout(new int[]{1,3,7,9})); //layout 2
    layouts.add(new Layout(new int[]{4,6})); //layout 3
    layouts.add(new Layout(new int[]{2,8})); //etc.
    layouts.add(new Layout(new int[]{4,3,9}));
    layouts.add(new Layout(new int[]{1,7,6}));
    layouts.add(new Layout(new int[]{1,3,8}));
    layouts.add(new Layout(new int[]{2,7,9}));
    layouts.add(new Layout(new int[]{4,11,12,13,14}));
    layouts.add(new Layout(new int[]{4,15,16,17,18}));
    layouts.add(new Layout(new int[]{1,7,11,12,13,14}));
    //layouts.add(new Layout(new int[]{1,7,15,16,17,18}));
    layouts.add(new Layout(new int[]{4,3,13,14}));
    
  }

  public void printLayouts(){
    for(int i = 0; i < layouts.size(); i++){
      println(layouts.get(i));
      for(int j = 0; j < layouts.get(i).myContainers.length; j++){
        // println(layouts.get(i).myContainers[j]);
        print(layouts.get(i).myContainers[j].x + ", ");
        print(layouts.get(i).myContainers[j].y + ", ");
        print(layouts.get(i).myContainers[j].w + ", ");
        println(layouts.get(i).myContainers[j].h);
      }
      println();
    }
  }

  public void setNewContainerLayout(int _newLayout){

    //find out how many active widgets we need...
    int numActiveWidgetsNeeded = layouts.get(_newLayout).myContainers.length;
    //calculate the number of current active widgets & keep track of which widgets are active
    int numActiveWidgets = 0;
    // ArrayList<int> activeWidgets = new ArrayList<int>();
    for(int i = 0; i < widgets.size(); i++){
      if(widgets.get(i).isActive){
        numActiveWidgets++; //increment numActiveWidgets
        // activeWidgets.add(i); //keep track of the active widget
      }
    }

    if(numActiveWidgets > numActiveWidgetsNeeded){ //if there are more active widgets than needed
      //shut some down
      int numToShutDown = numActiveWidgets - numActiveWidgetsNeeded;
      int counter = 0;
      println("Powering " + numToShutDown + " widgets down, and remapping.");
      for(int i = widgets.size()-1; i >= 0; i--){
        if(widgets.get(i).isActive && counter < numToShutDown){
          println("Deactivating widget [" + i + "]");
          widgets.get(i).isActive = false;
          counter++;
        }
      }

      //and map active widgets
      counter = 0;
      for(int i = 0; i < widgets.size(); i++){
        if(widgets.get(i).isActive){
          widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
          counter++;
        }
      }

    } else if(numActiveWidgetsNeeded > numActiveWidgets){ //if there are less active widgets than needed
      //power some up
      int numToPowerUp = numActiveWidgetsNeeded - numActiveWidgets;
      int counter = 0;
      println("Powering " + numToPowerUp + " widgets up, and remapping.");
      for(int i = 0; i < widgets.size(); i++){
        if(!widgets.get(i).isActive && counter < numToPowerUp){
          println("Activating widget [" + i + "]");
          widgets.get(i).isActive = true;
          counter++;
        }
      }

      //and map active widgets
      counter = 0;
      for(int i = 0; i < widgets.size(); i++){
        if(widgets.get(i).isActive){
          widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
          // widgets.get(i).screenResized(); // do this to make sure the container is updated
          counter++;
        }
      }

    } else{ //if there are the same mount
      //simply remap active widgets
      println("Remapping widgets.");
      int counter = 0;
      for(int i = 0; i < widgets.size(); i++){
        if(widgets.get(i).isActive){
          widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
          counter++;
        }
      }
    }
  }
};

//this is a global function for adding new widgets--and their children (timeSeries, FFT, headPlot, etc.)--to the WidgetManager's widget ArrayList
public void addWidget(Widget myNewWidget, ArrayList<Widget> w){
  w.add(myNewWidget);
}

//the Layout class is an orgnanizational tool ... a layout consists of a combination of containers ... refer to Container.pde
class Layout{

  Container[] myContainers;
  int[] containerInts;

  Layout(int[] _myContainers){ //when creating a new layout, you pass in the integer #s of the containers you want as part of the layout ... so if I pass in the array {5}, my layout is 1 container that takes up the whole GUI body
    //constructor stuff
    myContainers = new Container[_myContainers.length]; //make the myContainers array equal to the size of the incoming array of ints
    containerInts = new int[_myContainers.length];
    for(int i = 0; i < _myContainers.length; i++){
      myContainers[i] = container[_myContainers[i]];
      containerInts[i] = _myContainers[i];
    }
  }

  public Container getContainer(int _numContainer){
    if(_numContainer < myContainers.length){
      return myContainers[_numContainer];
    } else{
      println("tried to return a non-existant container...");
      return myContainers[myContainers.length-1];
    }
  }
};
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "DAC_GUI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
