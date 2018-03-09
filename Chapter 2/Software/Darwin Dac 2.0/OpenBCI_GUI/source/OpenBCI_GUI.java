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
import processing.net.*; 
import grafica.*; 
import java.lang.reflect.*; 
import java.io.InputStreamReader; 
import java.awt.MouseInfo; 
import java.lang.Process; 
import java.util.Random; 
import java.awt.Robot; 
import java.awt.AWTException; 
import netP5.*; 
import oscP5.*; 
import hypermedia.net.*; 
import java.nio.ByteBuffer; 
import edu.ucsd.sccn.LSL; 
import gifAnimation.*; 
import java.io.OutputStream; 
import controlP5.*; 
import java.text.DateFormat; 
import java.text.SimpleDateFormat; 
import java.io.OutputStream; 
import ddf.minim.analysis.*; 
import java.io.OutputStream; 
import java.awt.Desktop; 
import java.net.*; 
import java.awt.AWTException; 
import java.awt.Robot; 
import java.awt.event.KeyEvent; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class OpenBCI_GUI extends PApplet {


///////////////////////////////////////////////////////////////////////////////
//
//   GUI for controlling the ADS1299-based OpenBCI
//
//   Created: Chip Audette, Oct 2013 - May 2014
//   Modified: Conor Russomanno & Joel Murphy, August 2014 - Dec 2014
//   Modified (v2.0): Conor Russomanno & Joel Murphy (AJ Keller helped too), June 2016
//   Modified (v3.0) AJ Keller (Conor Russomanno & Joel Murphy & Wangshu), September 2017
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
 // For TCP networking

 // For callbacks
 // For input


// import java.net.InetAddress; // Used for ping, however not working right now.

 //used for simulating mouse clicks

 // for OSC
 // for OSC
 //for UDP
 //for UDP
 //for LSL



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

// added for training widget
boolean training = false;
boolean trainingMode = true;
//


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
Gif loadingGIF;
Gif loadingGIF_blue;

boolean initSystemThreadLock = false;

// ---- Define variables related to OpenBCI_GUI UDPMarker functionality
UDP udpRX;

//choose where to get the EEG data
final int DATASOURCE_CYTON = 0; // new default, data from serial with Accel data CHIP 2014-11-03
final int DATASOURCE_GANGLION = 1;  //looking for signal from OpenBCI board via Serial/COM port, no Aux data
final int DATASOURCE_PLAYBACKFILE = 2;  //playback from a pre-recorded text file
final int DATASOURCE_SYNTHETIC = 3;  //Synthetically generated data
public int eegDataSource = -1; //default to none of the options

final int INTERFACE_NONE = -1; // Used to indicate no choice made yet on interface
final int INTERFACE_SERIAL = 0; // Used only by cyton
final int INTERFACE_HUB_BLE = 1; // used only by ganglion
final int INTERFACE_HUB_WIFI = 2; // used by both cyton and ganglion

//here are variables that are used if loading input data from a CSV text file...double slash ("\\") is necessary to make a single slash
String playbackData_fname = "N/A"; //only used if loading input data from a file
// String playbackData_fname;  //leave blank to cause an "Open File" dialog box to appear at startup.  USEFUL!
float playback_speed_fac = 1.0f;  //make 1.0 for real-time.  larger for faster playback
int currentTableRowIndex = 0;
Table_CSV playbackData_table;
int nextPlayback_millis = -100; //any negative number

// Initialize boards for constants
Cyton cyton = new Cyton(); //dummy creation to get access to constants, create real one later
Ganglion ganglion = new Ganglion(); //dummy creation to get access to constants, create real one later
// Intialize interface protocols
InterfaceSerial iSerial = new InterfaceSerial();
Hub hub = new Hub(); //dummy creation to get access to constants, create real one later

String openBCI_portName = "N/A";  //starts as N/A but is selected from control panel to match your OpenBCI USB Dongle's serial/COM
int openBCI_baud = 115200; //baud rate from the Arduino

String ganglion_portName = "N/A";

String wifi_portName = "N/A";

final static String PROTOCOL_BLE = "ble";
final static String PROTOCOL_SERIAL = "serial";
final static String PROTOCOL_WIFI = "wifi";

////// ---- Define variables related to OpenBCI board operations
//Define number of channels from cyton...first EEG channels, then aux channels
int nchan = NCHAN_CYTON; //Normally, 8 or 16.  Choose a smaller number to show fewer on the GUI
int n_aux_ifEnabled = 3;  // this is the accelerometer data CHIP 2014-11-03
//define variables related to warnings to the user about whether the EEG data is nearly railed (and, therefore, of dubious quality)
DataStatus is_railed[];
final int threshold_railed = PApplet.parseInt(pow(2, 23)-1000);  //fully railed should be +/- 2^23, so set this threshold close to that value
final int threshold_railed_warn = PApplet.parseInt(pow(2, 23)*0.9f); //set a somewhat smaller value as the warning threshold
//OpenBCI SD Card setting (if eegDataSource == 0)
int sdSetting = 0; //0 = do not write; 1 = 5 min; 2 = 15 min; 3 = 30 min; etc...
String sdSettingString = "Do not write to SD";
//cyton data packet
int nDataBackBuff;
DataPacket_ADS1299 dataPacketBuff[]; //allocate later in InitSystem
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

int marker_indicater = 0;

// Calculate nPointsPerUpdate based on sampling rate and buffer update rate
// @update_millis: update the buffer every 40 milliseconds
// @nPointsPerUpdate: update the GUI after this many data points have been received.
// The sampling rate should be ideally a multiple of 25, so as to make actual buffer update rate exactly 40ms
final int update_millis = 40;
int nPointsPerUpdate;   // no longer final, calculate every time in initSystem
// final int nPointsPerUpdate = 50; //update the GUI after this many data points have been received
// final int nPointsPerUpdate = 24; //update the GUI after this many data points have been received
// final int nPointsPerUpdate = 10; //update the GUI after this many data points have been received

//define some data fields for handling data here in processing
float dataBuffX[];  //define the size later
float dataBuffY_uV[][]; //2D array to handle multiple data channels, each row is a new channel so that dataBuffY[3][] is channel 4
float dataBuffY_filtY_uV[][];
float yLittleBuff[];
float yLittleBuff_uV[][]; //small buffer used to send data to the filters
float accelerometerBuff[][]; // accelerometer buff 500 points
float auxBuff[][];
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

// Serial output
String serial_output_portName = "/dev/tty.usbmodem1421";  //must edit this based on the name of the serial/COM port
Serial serial_output;
int serial_output_baud = 9600; //baud rate from the Arduino

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
PImage darwinLogo;

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
// PulseSensor_Widget pulseWidget;

boolean no_start_connection = false;
boolean has_processed = false;
boolean isOldData = false;

int indices = 0;

boolean synthesizeData = false;

int timeOfSetup = 0;
boolean isHubInitialized = false;
boolean isHubObjectInitialized = false;
int bgColor = color(1, 18, 41);
int openbciBlue = color(31, 69, 110);
int COLOR_SCHEME_DEFAULT = 1;
int COLOR_SCHEME_ALTERNATIVE_A = 2;
// int COLOR_SCHEME_ALTERNATIVE_B = 3;
int colorScheme = COLOR_SCHEME_ALTERNATIVE_A;

Process nodeHubby;
int hubPid = 0;
String nodeHubName = "OpenBCIHub";
Robot rob3115;

PApplet ourApplet;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

//========================SETUP============================//

int frameRateCounter = 1; //0 = 24, 1 = 30, 2 = 45, 3 = 60

public void setup() {
  
  darwinLogo = loadImage("darwin-horizontal-blue2.png");
  if (!isWindows()) hubStop(); //kill any existing hubs before starting a new one..
  hubInit(); // putting down here gives windows time to close any open apps

  println("Welcome to the Processing-based OpenBCI GUI!"); //Welcome line.
  println("Last update: 9/5/2016"); //Welcome line.
  println("For more information about how to work with this code base, please visit: http://docs.openbci.com/OpenBCI%20Software/");
  //open window
  
  ourApplet = this;

  if(frameRateCounter==0){
    frameRate(24); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
  }
  if(frameRateCounter==1){
    frameRate(30); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
  }
  if(frameRateCounter==2){
    frameRate(45); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
  }
  if(frameRateCounter==3){
    frameRate(60); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
  }

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
        println("Darwin_DAC_GUI: setup: RESIZED");
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
  logo_white = loadImage("BCI_DarwinLarge_White.png");
  cog = loadImage("DarwinOpenBCICS.gif");
  loadingGIF = new Gif(this, "DarwinOpenBCICS.gif");
  loadingGIF.loop();
  loadingGIF_blue = new Gif(this, "DarwinOpenBCICS.gif");
  loadingGIF_blue.loop();

  playground = new Playground(navBarHeight);

  buttonHelpText = new ButtonHelpText();

  myPresentation = new Presentation();

  // UDPMarker functionality
  // Setup the UDP receiver
  int portRX = 51000;  // this is the UDP port the application will be listening on
  String ip = "127.0.0.1";  // Currently only localhost is supported as UDP Marker source

  //create new object for receiving
  udpRX=new UDP(this,portRX,ip);
  udpRX.setReceiveHandler("udpReceiveHandler");
  udpRX.log(true);
  udpRX.listen(true);
  // Print some useful diagnostics
  println("Darwin_DAC_GUI::Setup: Is RX mulitcast: "+udpRX.isMulticast());
  println("Darwin_DAC_GUI::Setup: Has RX joined multicast: "+udpRX.isJoined());

  timeOfSetup = millis(); //keep track of time when setup is finished... used to make sure enough time has passed before creating some other objects (such as the Ganglion instance)
  
  soundMinim = new Minim(this);
  // this loads mysong.wav from the data folder
  voice = soundMinim.loadFile(dataPath("welcome.mp3"));
  voice.play();
  
}
//====================== END-OF-SETUP ==========================//

//====================UDP Packet Handler==========================//
// This function handles the received UDP packet
// See the documentation for the Java UDP class here:
// https://ubaa.net/shared/processing/udp/udp_class_udp.htm

String udpReceiveString = null;

public void udpReceiveHandler(byte[] data, String ip, int portRX){

  String udpString = new String(data);
  println(udpString+" from: "+ip+" and port: "+portRX);
  if (udpString.length() >=5  && udpString.indexOf("MARK") >= 0){

    /*  Old version with 10 markers
    char c = value.charAt(4);
  if ( c>= '0' && c <= '9'){
      println("Found a valid UDP STIM of value: "+int(c)+" chr: "+c);
      hub.sendCommand("`"+char(c-(int)'0'));
      */
    int intValue = Integer.parseInt(udpString.substring(4));

    if (intValue > 0 && intValue < 96){ // Since we only send single char ascii value markers (from space to char(126)

      String sendString = "`"+PApplet.parseChar(intValue+31);

      println("Marker value: "+udpString+" with numeric value of char("+intValue+") as : "+sendString);
      hub.sendCommand(sendString);

    } else {
      println("udpReceiveHandler::Warning:invalid UDP STIM of value: "+intValue+" Received String: "+udpString);
    }
  } else {
      println("udpReceiveHandler::Warning:invalid UDP marker packet: "+udpString);

  }
}

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
  if (!isWindows()) {
    hubStart();
    prepareExitHandler();
  }
}

/**
 * Starts the node hub working, tested on mac and windows.
 */
public void hubStart() {
  println("Launching application from local data dir");
  try {
    // https://forum.processing.org/two/discussion/13053/use-launch-for-applications-kept-in-data-folder
    if (isWindows()) {
      println("Darwin_DAC_GUI: hubStart: OS Detected: Windows");
      nodeHubby = launch(dataPath("OpenBCIHub.exe"));
    } else if (isLinux()) {
      println("Darwin_DAC_GUI: hubStart: OS Detected: Linux");
      nodeHubby = exec(dataPath("OpenBCIHub"));
    } else {
      println("Darwin_DAC_GUI: hubStart: OS Detected: Mac");
      nodeHubby = launch(dataPath("OpenBCIHub.app"));
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
    Runtime.getRuntime().exec("taskkill /F /IM OpenBCIHub.exe");
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

  verbosePrint("Darwin_DAC_GUI: initSystem: -- Init 0 -- " + millis());
  timeOfInit = millis(); //store this for timeout in case init takes too long
  verbosePrint("timeOfInit = " + timeOfInit);

  //prepare data variables
  verbosePrint("Darwin_DAC_GUI: initSystem: Preparing data variables...");

  if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
    //open and load the data file
    println("Darwin_DAC_GUI: initSystem: loading playback data from " + playbackData_fname);
    try {
      playbackData_table = new Table_CSV(playbackData_fname);
      playbackData_table.removeColumn(0);
    } catch (Exception e) {
      println("Darwin_DAC_GUI: initSystem: could not open file for playback: " + playbackData_fname);
      println("   : quitting...");
      hub.killAndShowMsg("Could not open file for playback: " + playbackData_fname);
    }
    println("Darwin_DAC_GUI: initSystem: loading complete.  " + playbackData_table.getRowCount() + " rows of data, which is " + round(PApplet.parseFloat(playbackData_table.getRowCount())/getSampleRateSafe()) + " seconds of EEG data");
    //removing first column of data from data file...the first column is a time index and not eeg data

  }
  verbosePrint("Darwin_DAC_GUI: initSystem: Initializing core data objects");

  // Nfft = getNfftSafe();
  nDataBackBuff = 3*(int)getSampleRateSafe();
  dataPacketBuff = new DataPacket_ADS1299[nDataBackBuff]; // call the constructor here
  nPointsPerUpdate = PApplet.parseInt(round(PApplet.parseFloat(update_millis) * getSampleRateSafe()/ 1000.f));
  dataBuffX = new float[(int)(dataBuff_len_sec * getSampleRateSafe())];
  dataBuffY_uV = new float[nchan][dataBuffX.length];
  dataBuffY_filtY_uV = new float[nchan][dataBuffX.length];
  yLittleBuff = new float[nPointsPerUpdate];
  yLittleBuff_uV = new float[nchan][nPointsPerUpdate]; //small buffer used to send data to the filters
  auxBuff = new float[3][nPointsPerUpdate];
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
  dataProcessing = new DataProcessing(nchan, getSampleRateSafe());
  dataProcessing_user = new DataProcessing_User(nchan, getSampleRateSafe());

  //initialize the data
  prepareData(dataBuffX, dataBuffY_uV, getSampleRateSafe());

  verbosePrint("Darwin_DAC_GUI: initSystem: -- Init 1 -- " + millis());
  verbosePrint("Darwin_DAC_GUI: initSystem: Initializing FFT data objects");

  //initialize the FFT objects
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    // verbosePrint("Init FFT Buff \u2013 " + Ichan);
    fftBuff[Ichan] = new FFT(getNfftSafe(), getSampleRateSafe());
  }  //make the FFT objects

  initializeFFTObjects(fftBuff, dataBuffY_uV, getNfftSafe(), getSampleRateSafe());

  //prepare some signal processing stuff
  //for (int Ichan=0; Ichan < nchan; Ichan++) { detData_freqDomain[Ichan] = new DetectionData_FreqDomain(); }

  verbosePrint("Darwin_DAC_GUI: initSystem: -- Init 2 -- " + millis());
  verbosePrint("Darwin_DAC_GUI: initSystem: Closing ControlPanel...");

  controlPanel.close();
  topNav.controlPanelCollapser.setIsActive(false);
  verbosePrint("Darwin_DAC_GUI: initSystem: Initializing comms with hub....");
  hub.changeState(hub.STATE_COMINIT);
  // hub.searchDeviceStop();

  //prepare the source of the input data
  switch (eegDataSource) {
    case DATASOURCE_CYTON:
      int nEEDataValuesPerPacket = nchan;
      boolean useAux = true;
      if (cyton.getInterface() == INTERFACE_SERIAL) {
        cyton = new Cyton(this, openBCI_portName, openBCI_baud, nEEDataValuesPerPacket, useAux, n_aux_ifEnabled, cyton.getInterface()); //this also starts the data transfer after XX seconds
      } else {
        cyton = new Cyton(this, wifi_portName, openBCI_baud, nEEDataValuesPerPacket, useAux, n_aux_ifEnabled, cyton.getInterface()); //this also starts the data transfer after XX seconds
      }
      break;
    case DATASOURCE_SYNTHETIC:
      //do nothing
      break;
    case DATASOURCE_PLAYBACKFILE:
      break;
    case DATASOURCE_GANGLION:
      if (ganglion.getInterface() == INTERFACE_HUB_BLE) {
        hub.connectBLE(ganglion_portName);
      } else {
        hub.connectWifi(wifi_portName);
      }
      break;
    default:
      break;
    }

  verbosePrint("Darwin_DAC_GUI: initSystem: -- Init 3 -- " + millis());

  if (abandonInit) {
    haltSystem();
    println("Failed to connect to data source... 1");
    outputError("Failed to connect to data source fail point 1");
  } else {
    println("  3a -- " + millis());
    //initilize the GUI
    // initializeGUI(); //will soon be destroyed... and replaced with ...  wm = new WidgetManager(this);
    topNav.initSecondaryNav();
    println("  3b -- " + millis());

    //open data file
    if (eegDataSource == DATASOURCE_CYTON) openNewLogFile(fileName);  //open a new log file
    if (eegDataSource == DATASOURCE_GANGLION) openNewLogFile(fileName); // println("open ganglion output file");

    // wm = new WidgetManager(this);
    setupWidgetManager();

    if (!abandonInit) {
      println("  3c -- " + millis());
      // setupGUIWidgets(); //####

      nextPlayback_millis = millis(); //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
      w_timeSeries.hsc.loadDefaultChannelSettings();

      if (eegDataSource != DATASOURCE_GANGLION && eegDataSource != DATASOURCE_CYTON) {
        systemMode = SYSTEMMODE_POSTINIT; //tell system it's ok to leave control panel and start interfacing GUI
      }
      if (!abandonInit) {
        controlPanel.close();
      } else {
        haltSystem();
        println("Failed to connect to data source... 2");
        // output("Failed to connect to data source...");
      }
    } else {
      haltSystem();
      println("Failed to connect to data source... 3");
      // output("Failed to connect to data source...");
    }
  }

  verbosePrint("Darwin_DAC_GUI: initSystem: -- Init 4 -- " + millis());

  //reset init variables
  midInit = false;
  abandonInit = false;
}

/**
 * @description Useful function to get the correct sample rate based on data source
 * @returns `float` - The frequency / sample rate of the data source
 */
public float getSampleRateSafe() {
  if (eegDataSource == DATASOURCE_GANGLION) {
    return ganglion.getSampleRate();
  } else if (eegDataSource == DATASOURCE_CYTON){
    return cyton.getSampleRate();
  } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
    return playbackData_table.getSampleRate();
  } else {
    return 250;
  }
}

/**
* @description Get the correct points of FFT based on sampling rate
* @returns `int` - Points of FFT. 125Hz, 200Hz, 250Hz -> 256points. 1000Hz -> 1024points. 1600Hz -> 2048 points.
*/
public int getNfftSafe() {
  int sampleRate = (int)getSampleRateSafe();
  switch (sampleRate) {
    case 1000:
      return 1024;
    case 1600:
      return 2048;
    case 125:
    case 200:
    case 250:
    default:
      return 256;
  }
}

public void startRunning() {
  verbosePrint("startRunning...");
  output("Data stream started.");
  if (eegDataSource == DATASOURCE_GANGLION) {
    if (ganglion != null) {
      ganglion.startDataTransfer();
    }
  } else {
    if (cyton != null) {
      println("DEBUG: start data transfer");
      cyton.startDataTransfer();
    }
  }
  isRunning = true;
}

public void stopRunning() {
  // openBCI.changeState(0); //make sure it's no longer interpretting as binary
  verbosePrint("Darwin_DAC_GUI: stopRunning: stop running...");
  if (isRunning) {
    output("Data stream stopped.");
  }
  if (eegDataSource == DATASOURCE_GANGLION) {
    if (ganglion != null) {
      ganglion.stopDataTransfer();
    }
  } else {
    if (cyton != null) {
      cyton.stopDataTransfer();
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
    verbosePrint("Darwin_DAC_GUI: stopButton was pressed...stopping data transfer...");
    wm.setUpdating(false);
    stopRunning();
    topNav.stopButton.setString(topNav.stopButton_pressToStart_txt);
    topNav.stopButton.setColorNotPressed(color(184, 220, 105));
    if (eegDataSource == DATASOURCE_GANGLION && ganglion.isCheckingImpedance()) {
      ganglion.impedanceStop();
      w_ganglionImpedance.startStopCheck.but_txt = "Start Impedance Check";
    }
  } else { //not running
    verbosePrint("Darwin_DAC_GUI: startButton was pressed...starting data transfer...");
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


//halt the data collection
public void haltSystem() {
  println("Darwin_DAC_GUI: haltSystem: Halting system for reconfiguration of settings...");
  if (initSystemButton.but_txt == "STOP SYSTEM") {
    initSystemButton.but_txt = "START SYSTEM";
  }

  stopRunning();  //stop data transfer

  if(cyton.isPortOpen()) {
    if (w_pulsesensor.analogReadOn) {
      hub.sendCommand("/0");
      println("Stopping Analog Read to read accelerometer");
      w_pulsesensor.analogModeButton.setString("Turn Analog Read On");
      w_pulsesensor.analogReadOn = false;
    }
  }

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
  ganglion_portName = "N/A";
  wifi_portName = "N/A";

  controlPanel.resetListItems();

  if (eegDataSource == DATASOURCE_CYTON) {
    closeLogFile();  //close log file
    cyton.closeSDandPort();
  }
  if (eegDataSource == DATASOURCE_GANGLION) {
    if(ganglion.isCheckingImpedance()){
      ganglion.impedanceStop();
      w_ganglionImpedance.startStopCheck.but_txt = "Start Impedance Check";
    }
    closeLogFile();  //close log file
    ganglion.closePort();
  }
  systemMode = SYSTEMMODE_PREINIT;
  hub.changeState(hub.STATE_NOCOM);
  abandonInit = false;

  bleList.items.clear();
  wifiList.items.clear();

  if (ganglion.isBLE() || ganglion.isWifi() || cyton.isWifi()) {
    hub.searchDeviceStart();
  }
}

public void delayedInit() {
  // Initialize a plot
  GPlot plot = new GPlot(this);
}

public void systemUpdate() { // for updating data values and variables

  if (isHubInitialized && isHubObjectInitialized == false && millis() - timeOfSetup >= 1500) {
    hub = new Hub(this);
    println("Instantiating hub object...");
    isHubObjectInitialized = true;
    thread("delayedInit");
  }

  // //update the sync state with the OpenBCI hardware
  // if (iSerial.get_state() == iSerial.STATE_NOCOM || iSerial.get_state() == iSerial.STATE_COMINIT || iSerial.get_state() == iSerial.STATE_SYNCWITHHARDWARE) {
  //   iSerial.updateSyncState(sdSetting);
  // }

  // if (hub.get_state() == hub.STATE_NOCOM || hub.get_state() == hub.STATE_COMINIT || hub.get_state() == hub.STATE_SYNCWITHHARDWARE) {
  //   hub.updateSyncState(sdSetting);
  // }

  //prepare for updating the GUI
  win_x = width;
  win_y = height;

  helpWidget.update();
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
            println("Darwin_DAC_GUI: systemUpdate: New GUI reinitialize delay = " + reinitializeGUIdelay);
          }
        } else {
          println("Darwin_DAC_GUI: systemUpdate: reinitializing GUI after resize... not updating GUI");
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
      println("Darwin_DAC_GUI: setup: RESIZED");
      screenHasBeenResized = true;
      timeOfLastScreenResize = millis();
      widthOfLastScreen = width;
      heightOfLastScreen = height;
    }

    //re-initialize GUI if screen has been resized and it's been more than 1/2 seccond (to prevent reinitialization of GUI from happening too often)
    if (screenHasBeenResized) {
      // GUIWidgets_screenResized(width, height);
      ourApplet = this; //reset PApplet...
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

    if (!initSystemThreadLock) {
      if (wm.isWMInitialized) {
        wm.update();
        playground.update();
      }
    }
  }
}

public void systemDraw() { //for drawing to the screen


  // Conor's attempt at adjusting the GUI to be 2x in size for High DPI screens ... attempt failed
  // int currentWidth;
  // int currentHeight;
  // if(!highDPI){
  //   currentWidth = width;
  //   currentHeight = height;
  // }
  // if(highDPI){
  //   pushMatrix();
  //   scale(2);
  // }

  //redraw the screen...not every time, get paced by when data is being plotted
  background(bgColor);  //clear the screen
  noStroke();
  //background(255);  //clear the screen

  if (systemMode >= SYSTEMMODE_POSTINIT && !initSystemThreadLock) {
    int drawLoopCounter_thresh = 100;
    if ((redrawScreenNow) || (drawLoop_counter >= drawLoopCounter_thresh)) {
      //if (drawLoop_counter >= drawLoopCounter_thresh) println("Darwin_DAC_GUI: redrawing based on loop counter...");
      drawLoop_counter=0; //reset for next time
      redrawScreenNow = false;  //reset for next time

      //update the title of the figure;
      switch (eegDataSource) {
      case DATASOURCE_CYTON:
        switch (outputDataSource) {
        case OUTPUT_SOURCE_ODF:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps, " + PApplet.parseInt(PApplet.parseFloat(fileoutput_odf.getRowsWritten())/getSampleRateSafe()) + " secs Saved, Writing to " + output_fname);
          break;
        case OUTPUT_SOURCE_BDF:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps, " + PApplet.parseInt(fileoutput_bdf.getRecordsWritten()) + " secs Saved, Writing to " + output_fname);
          break;
        case OUTPUT_SOURCE_NONE:
        default:
          surface.setTitle(PApplet.parseInt(frameRate) + " fps");
          break;
        }
        break;
      case DATASOURCE_SYNTHETIC:
        surface.setTitle(PApplet.parseInt(frameRate) + " fps, Using Synthetic EEG Data");
        break;
      case DATASOURCE_PLAYBACKFILE:
        surface.setTitle(PApplet.parseInt(frameRate) + " fps, Playing " + PApplet.parseInt(PApplet.parseFloat(currentTableRowIndex)/getSampleRateSafe()) + " of " + PApplet.parseInt(PApplet.parseFloat(playbackData_table.getRowCount())/getSampleRateSafe()) + " secs, Reading from: " + playbackData_fname);
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
        println("Darwin_DAC_GUI: systemDraw: New GUI reinitialize delay = " + reinitializeGUIdelay);
      }
    } else {
      //reinitializing GUI after resize
      println("Darwin_DAC_GUI: systemDraw: reinitializing GUI after resize... not drawing GUI");
    }

    //dataProcessing_user.draw();
    drawContainers();
  } else { //systemMode != 10
    //still print title information about fps
    surface.setTitle(PApplet.parseInt(frameRate) + " fps \u2014 Darwin Ecosystem DAC GUI");
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

  if ((hub.get_state() == hub.STATE_COMINIT || hub.get_state() == hub.STATE_SYNCWITHHARDWARE) && systemMode == SYSTEMMODE_PREINIT) {
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
  if (drawPresentation) {
    myPresentation.draw();
    //emg_widget.drawTriggerFeedback();
    //dataProcessing_user.drawTriggerFeedback();
  }

  // use commented code below to verify frameRate and check latency
  // println("Time since start: " + millis() + " || Time since last frame: " + str(millis()-timeOfLastFrame));
  // timeOfLastFrame = millis();

  buttonHelpText.draw();
  mouseOutOfBounds(); // to fix


  // Conor's attempt at adjusting the GUI to be 2x in size for High DPI screens ... attempt failed
  // if(highDPI){
  //   popMatrix();
  //   size(currentWidth*2, currentHeight*2);
  // }

}

public void introAnimation() {
  pushStyle();
  imageMode(CENTER);
  background(255);
  int t1 = 4000;
  int t2 = 6000;
  int t3 = 8000;
  float transparency = 0;

  if (millis() >= t1) {
    transparency = map(millis(), t1, t2, 0, 255);
    tint(255, transparency);
    //draw OpenBCI Logo Front & Center
    image(cog, width/2, height/2, width/1.75f, width/6);
    textFont(p3, 16);
    textLeading(24);
    fill(31, 69, 110, transparency);
    textAlign(CENTER, CENTER);
    text("Darwin Ecosystem DAC GUI v3.2.0\nJanuary 2018", width/2, height/2 + width/9);
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

final char command_stop = 's';
// final String command_startText = "x";
final char command_startBinary = 'b';
final char command_startBinary_wAux = 'n';  // already doing this with 'b' now
final char command_startBinary_4chan = 'v';  // not necessary now
final char command_activateFilters = 'f';  // swithed from 'F' to 'f'  ... but not necessary because taken out of hardware code
final char command_deactivateFilters = 'g';  // not necessary anymore

final String command_setMode = "/";  // this is used to set the board into different modes

final char[] command_deactivate_channel = {'1', '2', '3', '4', '5', '6', '7', '8', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i'};
final char[] command_activate_channel = {'!', '@', '#', '$', '%', '^', '&', '*', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I'};

int channelDeactivateCounter = 0; //used for re-deactivating channels after switching settings...

final int BOARD_MODE_DEFAULT = 0;
final int BOARD_MODE_DEBUG = 1;
final int BOARD_MODE_ANALOG = 2;
final int BOARD_MODE_DIGITAL = 3;
final int BOARD_MODE_MARKER = 4;

//everything below is now deprecated...
// final String[] command_activate_leadoffP_channel = {'!', '@', '#', '$', '%', '^', '&', '*'};  //shift + 1-8
// final String[] command_deactivate_leadoffP_channel = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I'};   //letters (plus shift) right below 1-8
// final String[] command_activate_leadoffN_channel = {'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K'}; //letters (plus shift) below the letters below 1-8
// final String[] command_deactivate_leadoffN_channel = {'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<'};   //letters (plus shift) below the letters below the letters below 1-8
// final String command_biasAuto = "`";
// final String command_biasFixed = "~";

// ArrayList defaultChannelSettings;

//here is the routine that listens to the serial port.
//if any data is waiting, get it, parse it, and stuff it into our vector of
//pre-allocated dataPacketBuff

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------

class Cyton {

  private int nEEGValuesPerPacket = 8; //defined by the data format sent by cyton boards
  private int nAuxValuesPerPacket = 3; //defined by the data format sent by cyton boards
  private DataPacket_ADS1299 rawReceivedDataPacket;
  private DataPacket_ADS1299 missedDataPacket;
  private DataPacket_ADS1299 dataPacket;
  // public int [] validAuxValues = {0, 0, 0};
  // public boolean[] freshAuxValuesAvailable = {false, false, false};
  // public boolean freshAuxValues = false;
  //DataPacket_ADS1299 prevDataPacket;

  private int nAuxValues;
  private boolean isNewDataPacketAvailable = false;
  private OutputStream output; //for debugging  WEA 2014-01-26
  private int prevSampleIndex = 0;
  private int serialErrorCounter = 0;

  private final int fsHzSerialCyton = 250;  //sample rate used by OpenBCI board...set by its Arduino code
  private final int fsHzSerialCytonDaisy = 125;  //sample rate used by OpenBCI board...set by its Arduino code
  private final int fsHzWifi = 1000;  //sample rate used by OpenBCI board...set by its Arduino code
  private final int NfftSerialCyton = 256;
  private final int NfftSerialCytonDaisy = 256;
  private final int NfftWifi = 1024;
  private final float ADS1299_Vref = 4.5f;  //reference voltage for ADC in ADS1299.  set by its hardware
  private float ADS1299_gain = 24.0f;  //assumed gain setting for ADS1299.  set by its Arduino code
  private float openBCI_series_resistor_ohms = 2200; // Ohms. There is a series resistor on the 32 bit board.
  private float scale_fac_uVolts_per_count = ADS1299_Vref / ((float)(pow(2, 23)-1)) / ADS1299_gain  * 1000000.f; //ADS1299 datasheet Table 7, confirmed through experiment
  //float LIS3DH_full_scale_G = 4;  // +/- 4G, assumed full scale setting for the accelerometer
  private final float scale_fac_accel_G_per_count = 0.002f / ((float)pow(2, 4));  //assume set to +/4G, so 2 mG per digit (datasheet). Account for 4 bits unused
  //private final float scale_fac_accel_G_per_count = 1.0;  //to test stimulations  //final float scale_fac_accel_G_per_count = 1.0;
  private final float leadOffDrive_amps = 6.0e-9f;  //6 nA, set by its Arduino code

  boolean isBiasAuto = true; //not being used?

  private int curBoardMode = BOARD_MODE_DEFAULT;

  //data related to Conor's setup for V3 boards
  final char[] EOT = {'$', '$', '$'};
  char[] prev3chars = {'#', '#', '#'};
  public String potentialFailureMessage = "";
  public String defaultChannelSettings = "";
  public String daisyOrNot = "";
  public int hardwareSyncStep = 0; //start this at 0...
  private long timeOfLastCommand = 0; //used when sync'ing to hardware

  private int curInterface = INTERFACE_SERIAL;
  private int sampleRate = fsHzWifi;
  PApplet mainApplet;

  //some get methods
  public float getSampleRate() {
    if (isSerial()) {
      if (nchan == NCHAN_CYTON_DAISY) {
        return fsHzSerialCytonDaisy;
      } else {
        return fsHzSerialCyton;
      }
    } else {
      return hub.getSampleRate();
    }
  }

  // TODO: ADJUST getNfft for new sample variable sample rates
  public int getNfft() {
    if (isWifi()) {
      if (sampleRate == fsHzSerialCyton) {
        return NfftSerialCyton;
      } else {
        return NfftWifi;
      }
    } else {
      if (nchan == NCHAN_CYTON_DAISY) {
        return NfftSerialCytonDaisy;
      } else {
        return NfftSerialCyton;
      }
    }
  }
  public int getBoardMode() {
    return curBoardMode;
  }
  public int getInterface() {
    return curInterface;
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

  public void setBoardMode(int boardMode) {
    hub.sendCommand("/" + boardMode);
    curBoardMode = boardMode;
    print("Cyton: setBoardMode to :" + curBoardMode);
  }

  public void setSampleRate(int _sampleRate) {
    sampleRate = _sampleRate;
    output("Setting sample rate for Cyton to " + sampleRate + "Hz");
    println("Setting sample rate for Cyton to " + sampleRate + "Hz");
    hub.setSampleRate(sampleRate);
  }

  public boolean setInterface(int _interface) {
    curInterface = _interface;
    // println("current interface: " + curInterface);
    println("setInterface: curInterface: " + getInterface());
    if (isWifi()) {
      setSampleRate((int)fsHzWifi);
      hub.setProtocol(PROTOCOL_WIFI);
    } else if (isSerial()) {
      setSampleRate((int)fsHzSerialCyton);
      hub.setProtocol(PROTOCOL_SERIAL);
    }
    return true;
  }

  //constructors
  Cyton() {
  };  //only use this if you simply want access to some of the constants
  Cyton(PApplet applet, String comPort, int baud, int nEEGValuesPerOpenBCI, boolean useAux, int nAuxValuesPerOpenBCI, int _interface) {
    curInterface = _interface;

    initDataPackets(nEEGValuesPerOpenBCI, nAuxValuesPerOpenBCI);

    if (isSerial()) {
      hub.connectSerial(comPort);
    } else if (isWifi()) {
      hub.connectWifi(comPort);
    }
  }

  public void initDataPackets(int _nEEGValuesPerPacket, int _nAuxValuesPerPacket) {
    nEEGValuesPerPacket = _nEEGValuesPerPacket;
    nAuxValuesPerPacket = _nAuxValuesPerPacket;
    //allocate space for data packet
    rawReceivedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    missedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    dataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);            //this could be 8 or 16 channels
    //set all values to 0 so not null

    for (int i = 0; i < nEEGValuesPerPacket; i++) {
      rawReceivedDataPacket.values[i] = 0;
      //prevDataPacket.values[i] = 0;
    }

    for (int i=0; i < nEEGValuesPerPacket; i++) {
      dataPacket.values[i] = 0;
      missedDataPacket.values[i] = 0;
    }
    for (int i = 0; i < nAuxValuesPerPacket; i++) {
      rawReceivedDataPacket.auxValues[i] = 0;
      dataPacket.auxValues[i] = 0;
      missedDataPacket.auxValues[i] = 0;
      //prevDataPacket.auxValues[i] = 0;
    }
  }

  public int closeSDandPort() {
    closeSDFile();
    return closePort();
  }

  public int closePort() {
    if (isSerial()) {
      return hub.disconnectSerial();
    } else {
      return hub.disconnectWifi();
    }
  }

  public int closeSDFile() {
    println("Closing any open SD file. Writing 'j' to OpenBCI.");
    if (isPortOpen()) write('j'); // tell the SD file to close if one is open...
    delay(100); //make sure 'j' gets sent to the board
    return 0;
  }

  public void syncWithHardware(int sdSetting) {
    switch (hardwareSyncStep) {
      case 1: //send # of channels (8 or 16) ... (regular or daisy setup)
        println("Cyton: syncWithHardware: [1] Sending channel count (" + nchan + ") to OpenBCI...");
        if (nchan == 8) {
          write('c');
        }
        if (nchan == 16) {
          write('C', false);
        }
        break;
      case 2: //reset hardware to default registers
        println("Cyton: syncWithHardware: [2] Reseting OpenBCI registers to default... writing \'d\'...");
        write('d'); // TODO: Why does this not get a $$$ readyToSend = false?
        break;
      case 3: //ask for series of channel setting ASCII values to sync with channel setting interface in GUI
        println("Cyton: syncWithHardware: [3] Retrieving OpenBCI's channel settings to sync with GUI... writing \'D\'... waiting for $$$...");
        write('D', false); //wait for $$$ to iterate... applies to commands expecting a response
        break;
      case 4: //check existing registers
        println("Cyton: syncWithHardware: [4] Retrieving OpenBCI's full register map for verification... writing \'?\'... waiting for $$$...");
        write('?', false); //wait for $$$ to iterate... applies to commands expecting a response
        break;
      case 5:
        // write("j"); // send OpenBCI's 'j' commaned to make sure any already open SD file is closed before opening another one...
        switch (sdSetting) {
          case 1: //"5 min max"
            write('A', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 2: //"5 min max"
            write('S', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 3: //"5 min max"
            write('F', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 4: //"5 min max"
            write('G', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 5: //"5 min max"
            write('H', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 6: //"5 min max"
            write('J', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 7: //"5 min max"
            write('K', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          case 8: //"5 min max"
            write('L', false); //wait for $$$ to iterate... applies to commands expecting a response
            break;
          default:
            break; // Do Nothing
        }
        println("Cyton: syncWithHardware: [5] Writing selected SD setting (" + sdSettingString + ") to OpenBCI...");
        //final hacky way of abandoning initiation if someone selected daisy but doesn't have one connected.
        if(abandonInit){
          haltSystem();
          output("No daisy board present. Make sure you selected the correct number of channels.");
          controlPanel.open();
          abandonInit = false;
        }
        break;
      case 6:
        output("Cyton: syncWithHardware: The GUI is done intializing. Click outside of the control panel to interact with the GUI.");
        hub.changeState(hub.STATE_STOPPED);
        systemMode = 10;
        controlPanel.close();
        topNav.controlPanelCollapser.setIsActive(false);
        //renitialize GUI if nchan has been updated... needs to be built
        break;
    }
  }

  public void writeCommand(String val) {
    if (hub.isHubRunning()) {
      hub.write(String.valueOf(val));
    }
  }

  public boolean write(char val) {
    if (hub.isHubRunning()) {
      hub.sendCommand(val);
      return true;
    }
    return false;
  }

  public boolean write(char val, boolean _readyToSend) {
    // if (isSerial()) {
    //   iSerial.setReadyToSend(_readyToSend);
    // }
    return write(val);
  }

  public boolean write(String out, boolean _readyToSend) {
    // if (isSerial()) {
    //   iSerial.setReadyToSend(_readyToSend);
    // }
    return write(out);
  }

  public boolean write(String out) {
    if (hub.isHubRunning()) {
      hub.write(out);
      return true;
    }
    return false;
  }

  private boolean isSerial () {
    // println("My interface is " + curInterface);
    return curInterface == INTERFACE_SERIAL;
  }

  private boolean isWifi () {
    return curInterface == INTERFACE_HUB_WIFI;
  }

  public void startDataTransfer() {
    if (isPortOpen()) {
      // Now give the command to start binary data transmission
      if (isSerial()) {
        hub.changeState(hub.STATE_NORMAL);  // make sure it's now interpretting as binary
        println("Cyton: startDataTransfer(): writing \'" + command_startBinary + "\' to the serial port...");
        // if (isSerial()) iSerial.clear();  // clear anything in the com port's buffer
        write(command_startBinary);
      } else if (isWifi()) {
        println("Cyton: startDataTransfer(): writing \'" + command_startBinary + "\' to the wifi shield...");
        write(command_startBinary);
      }

    } else {
      println("port not open");
    }
  }

  public void stopDataTransfer() {
    if (isPortOpen()) {
      hub.changeState(hub.STATE_STOPPED);  // make sure it's now interpretting as binary
      println("Cyton: startDataTransfer(): writing \'" + command_stop + "\' to the serial port...");
      write(command_stop);// + "\n");
    }
  }

  public void printRegisters() {
    if (isPortOpen()) {
      println("Cyton: printRegisters(): Writing ? to OpenBCI...");
      write('?');
    }
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

  private boolean isPortOpen() {
    if (isWifi() || isSerial()) {
      return hub.isPortOpen();
    } else {
      return false;
    }
  }


  //activate or deactivate an EEG channel...channel counting is zero through nchan-1
  public void changeChannelState(int Ichan, boolean activate) {
    if (isPortOpen()) {
      // if ((Ichan >= 0) && (Ichan < command_activate_channel.length)) {
      if ((Ichan >= 0)) {
        if (activate) {
          // write(command_activate_channel[Ichan]);
          // gui.cc.powerUpChannel(Ichan);
          w_timeSeries.hsc.powerUpChannel(Ichan);
        } else {
          // write(command_deactivate_channel[Ichan]);
          // gui.cc.powerDownChannel(Ichan);
          w_timeSeries.hsc.powerDownChannel(Ichan);
        }
      }
    }
  }

  //deactivate an EEG channel...channel counting is zero through nchan-1
  public void deactivateChannel(int Ichan) {
    if (isPortOpen()) {
      if ((Ichan >= 0) && (Ichan < command_deactivate_channel.length)) {
        write(command_deactivate_channel[Ichan]);
      }
    }
  }

  //activate an EEG channel...channel counting is zero through nchan-1
  public void activateChannel(int Ichan) {
    if (isPortOpen()) {
      if ((Ichan >= 0) && (Ichan < command_activate_channel.length)) {
        write(command_activate_channel[Ichan]);
      }
    }
  }

  //return the state
  public boolean isStateNormal() {
    if (hub.get_state() == hub.STATE_NORMAL) {
      return true;
    } else {
      return false;
    }
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
    return dataPacket.copyTo(target);
  }


  private long timeOfLastChannelWrite = 0;
  private int channelWriteCounter = 0;
  private boolean isWritingChannel = false;

  public void configureAllChannelsToDefault() {
    write('d');
  };

  public void initChannelWrite(int _numChannel) {  //numChannel counts from zero
    timeOfLastChannelWrite = millis();
    isWritingChannel = true;
  }

  public void syncChannelSettings() {
    write("r,start" + hub.TCP_STOP);
  }

  /**
   * Used to convert a gain from the hub back into local codes.
   */
  public char getCommandForGain(int gain) {
    switch (gain) {
      case 1:
        return '0';
      case 2:
        return '1';
      case 4:
        return '2';
      case 6:
        return '3';
      case 8:
        return '4';
      case 12:
        return '5';
      case 24:
      default:
        return '6';
    }
  }

  /**
   * Used to convert raw code to hub code
   * @param inputType {String} - The input from a hub sync channel with register settings
   */
  public char getCommandForInputType(String inputType) {
    if (inputType.equals("normal")) return '0';
    if (inputType.equals("shorted")) return '1';
    if (inputType.equals("biasMethod")) return '2';
    if (inputType.equals("mvdd")) return '3';
    if (inputType.equals("temp")) return '4';
    if (inputType.equals("testsig")) return '5';
    if (inputType.equals("biasDrp")) return '6';
    if (inputType.equals("biasDrn")) return '7';
    return '0';
  }

  /**
   * Used to convert a local channel code into a hub gain which is human
   *  readable and in scientific values.
   */
  public int getGainForCommand(char cmd) {
    switch (cmd) {
      case '0':
        return 1;
      case '1':
        return 2;
      case '2':
        return 4;
      case '3':
        return 6;
      case '4':
        return 8;
      case '5':
        return 12;
      case '6':
      default:
        return 24;
    }
  }

  /**
   * Used right before a channel setting command is sent to the hub to convert
   *  local values into the expected form for the hub.
   */
  public String getInputTypeForCommand(char cmd) {
    final String inputTypeShorted = "shorted";
    final String inputTypeBiasMethod = "biasMethod";
    final String inputTypeMvdd = "mvdd";
    final String inputTypeTemp = "temp";
    final String inputTypeTestsig = "testsig";
    final String inputTypeBiasDrp = "biasDrp";
    final String inputTypeBiasDrn = "biasDrn";
    final String inputTypeNormal = "normal";
    switch (cmd) {
      case '1':
        return inputTypeShorted;
      case '2':
        return inputTypeBiasMethod;
      case '3':
        return inputTypeMvdd;
      case '4':
        return inputTypeTemp;
      case '5':
        return inputTypeTestsig;
      case '6':
        return inputTypeBiasDrp;
      case '7':
        return inputTypeBiasDrn;
      case '0':
      default:
        return inputTypeNormal;
    }
  }

  /**
   * Used to convert a local index number to a hub human readable sd setting
   *  command.
   */
  public String getSDSettingForSetting(int setting) {
    switch (setting) {
      case 1:
        return "5min";
      case 2:
        return "15min";
      case 3:
        return "30min";
      case 4:
        return "1hour";
      case 5:
        return "2hour";
      case 6:
        return "4hour";
      case 7:
        return "12hour";
      case 8:
        return "24hour";
      default:
        return "";
    }
  }

  // FULL DISCLAIMER: this method is messy....... very messy... we had to brute force a firmware miscue
  public void writeChannelSettings(int _numChannel, char[][] channelSettingValues) {   //numChannel counts from zero
    String output = "r,set,";
    output += Integer.toString(_numChannel) + ","; // 0 indexed channel number
    output += channelSettingValues[_numChannel][0] + ","; // power down
    output += getGainForCommand(channelSettingValues[_numChannel][1]) + ","; // gain
    output += getInputTypeForCommand(channelSettingValues[_numChannel][2]) + ",";
    output += channelSettingValues[_numChannel][3] + ",";
    output += channelSettingValues[_numChannel][4] + ",";
    output += channelSettingValues[_numChannel][5] + hub.TCP_STOP;
    write(output);
    // verbosePrint("done writing channel.");
    isWritingChannel = false;
  }

  private long timeOfLastImpWrite = 0;
  private int impWriteCounter = 0;
  private boolean isWritingImp = false;
  public boolean get_isWritingImp() {
    return isWritingImp;
  }

  // public void initImpWrite(int _numChannel) {  //numChannel counts from zero
  //   timeOfLastImpWrite = millis();
  //   isWritingImp = true;
  // }

  public void writeImpedanceSettings(int _numChannel, char[][] impedanceCheckValues) {  //numChannel counts from zero
    String output = "i,set,";
    if (_numChannel < 8) {
      output += (char)('0'+(_numChannel+1)) + ",";
    } else { //(_numChannel >= 8) {
      //command_activate_channel holds non-daisy and daisy values
      output += command_activate_channel[_numChannel] + ",";
    }
    output += impedanceCheckValues[_numChannel][0] + ",";
    output += impedanceCheckValues[_numChannel][1] + hub.TCP_STOP;
    write(output);
    isWritingImp = false;
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

class Ganglion {
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

  private int nEEGValuesPerPacket = NCHAN_GANGLION; // Defined by the data format sent by cyton boards
  private int nAuxValuesPerPacket = NUM_ACCEL_DIMS; // Defined by the arduino code

  private final float fsHzBLE = 200.0f;  //sample rate used by OpenBCI Ganglion board... set by its Arduino code
  private final float fsHzWifi = 1600.0f;  //sample rate used by OpenBCI Ganglion board on wifi, set by hub
  private final int NfftBLE = 256;
  private final int NfftWifi = 2048;
  private final float MCP3912_Vref = 1.2f;  // reference voltage for ADC in MCP3912 set in hardware
  private float MCP3912_gain = 1.0f;  //assumed gain setting for MCP3912.  NEEDS TO BE ADJUSTABLE JM
  private float scale_fac_uVolts_per_count = (MCP3912_Vref * 1000000.f) / (8388607.0f * MCP3912_gain * 1.5f * 51.0f); //MCP3912 datasheet page 34. Gain of InAmp = 80
  // private float scale_fac_accel_G_per_count = 0.032;
  private float scale_fac_accel_G_per_count_ble = 0.016f;
  private float scale_fac_accel_G_per_count_wifi = 0.001f;
  // private final float scale_fac_accel_G_per_count = 0.002 / ((float)pow(2,4));  //assume set to +/4G, so 2 mG per digit (datasheet). Account for 4 bits unused
  // private final float leadOffDrive_amps = 6.0e-9;  //6 nA, set by its Arduino code

  private int curInterface = INTERFACE_NONE;

  private DataPacket_ADS1299 dataPacket;

  private boolean connected = false;

  public int numberOfDevices = 0;
  public int maxNumberOfDevices = 10;

  private boolean checkingImpedance = false;
  private boolean accelModeActive = false;

  public boolean impedanceUpdated = false;
  public int[] impedanceArray = new int[NCHAN_GANGLION + 1];

  private int sampleRate = (int)fsHzWifi;

  // Getters
  public float getSampleRate() {
    if (isBLE()) {
      return fsHzBLE;
    } else {
      return hub.getSampleRate();
    }
  }
  public int getNfft() {
    if (isWifi()) {
      if (hub.getSampleRate() == (int)fsHzBLE) {
        return NfftBLE;
      } else {
        return NfftWifi;
      }
    } else {
      return NfftBLE;
    }
  }
  public float get_scale_fac_uVolts_per_count() { return scale_fac_uVolts_per_count; }
  public float get_scale_fac_accel_G_per_count() {
    if (isWifi()) {
      return scale_fac_accel_G_per_count_wifi;
    } else {
      return scale_fac_accel_G_per_count_ble;
    }
  }
  public boolean isCheckingImpedance() { return checkingImpedance; }
  public boolean isAccelModeActive() { return accelModeActive; }
  public void overrideCheckingImpedance(boolean val) { checkingImpedance = val; }
  public int getInterface() {
    return curInterface;
  }
  public boolean isBLE () {
    return curInterface == INTERFACE_HUB_BLE;
  }

  public boolean isWifi () {
    return curInterface == INTERFACE_HUB_WIFI;
  }

  public boolean isPortOpen() {
    return hub.isPortOpen();
  }

  private PApplet mainApplet;

  //constructors
  Ganglion() {};  //only use this if you simply want access to some of the constants
  Ganglion(PApplet applet) {
    mainApplet = applet;

    initDataPackets(nEEGValuesPerPacket, nAuxValuesPerPacket);
  }

  public void initDataPackets(int _nEEGValuesPerPacket, int _nAuxValuesPerPacket) {
    nEEGValuesPerPacket = _nEEGValuesPerPacket;
    nAuxValuesPerPacket = _nAuxValuesPerPacket;
    // For storing data into
    dataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    for(int i = 0; i < nEEGValuesPerPacket; i++) {
      dataPacket.values[i] = 0;
    }
    for(int i = 0; i < nAuxValuesPerPacket; i++){
      dataPacket.auxValues[i] = 0;
    }
  }

  private void handleError(int code, String msg) {
    output("Code " + code + "Error: " + msg);
    println("Code " + code + "Error: " + msg);
  }

  public void processImpedance(String msg) {
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
          println("Impedance for channel " + channel + " is " + value + " ohms.");
        }
      }
    }
  }

  public void setSampleRate(int _sampleRate) {
    sampleRate = _sampleRate;
    hub.setSampleRate(sampleRate);
    output("Setting sample rate for Ganglion to " + sampleRate + "Hz");
  }

  public void setInterface(int _interface) {
    curInterface = _interface;
    if (isBLE()) {
      setSampleRate((int)fsHzBLE);
      hub.setProtocol(PROTOCOL_BLE);
      // hub.searchDeviceStart();
    } else if (isWifi()) {
      setSampleRate((int)fsHzWifi);
      hub.setProtocol(PROTOCOL_WIFI);
      hub.searchDeviceStart();
    }
  }

  public int copyDataPacketTo(DataPacket_ADS1299 target) {
    return dataPacket.copyTo(target);
  }

  // SCANNING/SEARHING FOR DEVICES
  public int closePort() {
    if (isBLE()) {
      hub.disconnectBLE();
    } else if (isWifi()) {
      hub.disconnectWifi();
    }
    return 0;
  }

  /**
   * @description Sends a start streaming command to the Ganglion Node module.
   */
  public void startDataTransfer(){
    hub.changeState(hub.STATE_NORMAL);  // make sure it's now interpretting as binary
    println("Ganglion: startDataTransfer(): sending \'" + command_startBinary);
    if (checkingImpedance) {
      impedanceStop();
      delay(100);
      hub.sendCommand('b');
    } else {
      hub.sendCommand('b');
    }
  }

  /**
   * @description Sends a stop streaming command to the Ganglion Node module.
   */
  public void stopDataTransfer() {
    hub.changeState(hub.STATE_STOPPED);  // make sure it's now interpretting as binary
    println("Ganglion: stopDataTransfer(): sending \'" + command_stop);
    hub.sendCommand('s');
  }

  private void printGanglion(String msg) {
    print("Ganglion: "); println(msg);
  }

  // Channel setting
  //activate or deactivate an EEG channel...channel counting is zero through nchan-1
  public void changeChannelState(int Ichan, boolean activate) {
    if (isPortOpen()) {
      if ((Ichan >= 0)) {
        if (activate) {
          println("Ganglion: changeChannelState(): activate: sending " + command_activate_channel[Ichan]);
          hub.sendCommand(command_activate_channel[Ichan]);
          w_timeSeries.hsc.powerUpChannel(Ichan);
        } else {
          println("Ganglion: changeChannelState(): deactivate: sending " + command_deactivate_channel[Ichan]);
          hub.sendCommand(command_deactivate_channel[Ichan]);
          w_timeSeries.hsc.powerDownChannel(Ichan);
        }
      }
    }
  }

  /**
   * Used to start accel data mode. Accel arrays will arrive asynchronously!
   */
  public void accelStart() {
    println("Ganglion: accell: START");
    hub.write(TCP_CMD_ACCEL + "," + TCP_ACTION_START + TCP_STOP);
    accelModeActive = true;
  }

  /**
   * Used to stop accel data mode. Some accel arrays may arrive after stop command
   *  was sent by this function.
   */
  public void accelStop() {
    println("Ganglion: accel: STOP");
    hub.write(TCP_CMD_ACCEL + "," + TCP_ACTION_STOP + TCP_STOP);
    accelModeActive = false;
  }

  /**
   * Used to start impedance testing. Impedances will arrive asynchronously!
   */
  public void impedanceStart() {
    println("Ganglion: impedance: START");
    hub.write(TCP_CMD_IMPEDANCE + "," + TCP_ACTION_START + TCP_STOP);
    checkingImpedance = true;
  }

  /**
   * Used to stop impedance testing. Some impedances may arrive after stop command
   *  was sent by this function.
   */
  public void impedanceStop() {
    println("Ganglion: impedance: STOP");
    hub.write(TCP_CMD_IMPEDANCE + "," + TCP_ACTION_STOP + TCP_STOP);
    checkingImpedance = false;
  }

  /**
   * Puts the ganglion in bootloader mode.
   */
  public void enterBootloaderMode() {
    println("Ganglion: Entering Bootloader Mode");
    hub.sendCommand(GANGLION_BOOTLOADER_MODE.charAt(0));
    delay(500);
    closePort();
    haltSystem();
    initSystemButton.setString("START SYSTEM");
    controlPanel.open();
    output("Ganglion now in bootloader mode! Enjoy!");
  }
};
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
    println("OpenBCI_GUI: setup: RESIZED");
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
      println("CallbackListener: controlEvent: clearing cyton");
      cp5.get(Textfield.class, "fileName").clear();
      // cp5.get(Textfield.class, "fileNameGanglion").clear();

    } else if (cp5.isMouseOver(cp5.get(Textfield.class, "fileNameGanglion"))){
      println("CallbackListener: controlEvent: clearing ganglion");
      cp5.get(Textfield.class, "fileNameGanglion").clear();
    }
  }
};

MenuList sourceList;

//Global buttons and elements for the control panel (changed within the classes below)
MenuList serialList;
String[] serialPorts = new String[Serial.list().length];

MenuList bleList;
MenuList wifiList;

MenuList sdTimes;

MenuList channelList;

MenuList pollList;

int boxColor = color(200);
int boxStrokeColor = color(bgColor);
int isSelected_color = color(184, 220, 105);

// Button openClosePort;
// boolean portButtonPressed;

boolean calledForBLEList = false;
boolean calledForWifiList = false;

Button refreshPort;
Button refreshBLE;
Button refreshWifi;
Button protocolSerialCyton;
Button protocolWifiCyton;
Button protocolWifiGanglion;
Button protocolBLEGanglion;
// Button autoconnect;
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
Button popOutRadioConfigButton;
Button popOutWifiConfigButton;

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

Button eraseCredentials;
Button getIpAddress;
Button getFirmwareVersion;
Button getMacAddress;
Button getTypeOfAttachedBoard;
Button sampleRate200;
Button sampleRate250;
Button sampleRate500;
Button sampleRate1000;
Button sampleRate1600;
Button latencyCyton5ms;
Button latencyCyton10ms;
Button latencyCyton20ms;
Button latencyGanglion5ms;
Button latencyGanglion10ms;
Button latencyGanglion20ms;
Button wifiInternetProtocolCytonTCP;
Button wifiInternetProtocolCytonUDP;
Button wifiInternetProtocolCytonUDPBurst;
Button wifiInternetProtocolGanglionTCP;
Button wifiInternetProtocolGanglionUDP;
Button wifiInternetProtocolGanglionUDPBurst;

Button synthChanButton4;
Button synthChanButton8;
Button synthChanButton16;

Button playbackChanButton4;
Button playbackChanButton8;
Button playbackChanButton16;

Serial board;

ChannelPopup channelPopup;
PollPopup pollPopup;
RadioConfigBox rcBox;

WifiConfigBox wcBox;

//------------------------------------------------------------------------
//                       Global Functions
//------------------------------------------------------------------------

public void controlEvent(ControlEvent theEvent) {

  if (theEvent.isFrom("sourceList")) {
    // THIS IS TRIGGERED WHEN A USER SELECTS 'LIVE (from Cyton) or LIVE (from Ganglion), etc...'
    controlPanel.hideAllBoxes();

    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    String str = (String)bob.get("headline");
    int newDataSource = PApplet.parseInt(theEvent.getValue());

    if (newDataSource != DATASOURCE_SYNTHETIC && newDataSource != DATASOURCE_PLAYBACKFILE && !hub.nodeProcessHandshakeComplete) {
      if (isWindows()) {
        output("Please launch OpenBCI Hub prior to launching this application. Learn at docs.openbci.com", OUTPUT_LEVEL_ERROR);
      } else {
        output("Unable to establish link to Hub. Checkout tutorial at docs.openbci.com/OpenBCI%20Software/01-OpenBCI_GUI", OUTPUT_LEVEL_ERROR);
      }
      eegDataSource = -1;
      return;
    }

    protocolBLEGanglion.color_notPressed = autoFileName.color_notPressed;
    protocolWifiGanglion.color_notPressed = autoFileName.color_notPressed;
    protocolWifiCyton.color_notPressed = autoFileName.color_notPressed;
    protocolSerialCyton.color_notPressed = autoFileName.color_notPressed;

    eegDataSource = newDataSource; // reset global eegDataSource to the selected value from the list


    ganglion.setInterface(INTERFACE_NONE);
    cyton.setInterface(INTERFACE_NONE);

    if(newDataSource == DATASOURCE_CYTON){
      updateToNChan(8);
      chanButton8.color_notPressed = isSelected_color;
      chanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
      latencyCyton5ms.color_notPressed = autoFileName.color_notPressed;
      latencyCyton10ms.color_notPressed = isSelected_color;
      latencyCyton20ms.color_notPressed = autoFileName.color_notPressed;
      hub.setLatency(hub.LATENCY_10_MS);
      wifiInternetProtocolCytonTCP.color_notPressed = isSelected_color;
      wifiInternetProtocolCytonUDP.color_notPressed = autoFileName.color_notPressed;
      wifiInternetProtocolCytonUDPBurst.color_notPressed = autoFileName.color_notPressed;
      hub.setWifiInternetProtocol(hub.TCP);
    } else if(newDataSource == DATASOURCE_GANGLION){
      updateToNChan(4);
      if (isWindows() && isHubInitialized == false) {
        hubInit();
        timeOfSetup = millis();
      }
      latencyGanglion5ms.color_notPressed = autoFileName.color_notPressed;
      latencyGanglion10ms.color_notPressed = isSelected_color;
      latencyGanglion20ms.color_notPressed = autoFileName.color_notPressed;
      hub.setLatency(hub.LATENCY_10_MS);
      wifiInternetProtocolGanglionTCP.color_notPressed = isSelected_color;
      wifiInternetProtocolGanglionUDP.color_notPressed = autoFileName.color_notPressed;
      wifiInternetProtocolGanglionUDPBurst.color_notPressed = autoFileName.color_notPressed;
      hub.setWifiInternetProtocol(hub.TCP);
    } else if(newDataSource == DATASOURCE_PLAYBACKFILE){
      updateToNChan(8);
      playbackChanButton4.color_notPressed = autoFileName.color_notPressed;
      playbackChanButton8.color_notPressed = isSelected_color;
      playbackChanButton16.color_notPressed = autoFileName.color_notPressed;
    } else if(newDataSource == DATASOURCE_SYNTHETIC){
      updateToNChan(8);
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

  if (theEvent.isFrom("wifiList")) {
    Map bob = ((MenuList)theEvent.getController()).getItem(PApplet.parseInt(theEvent.getValue()));
    wifi_portName = (String)bob.get("headline");
    output("Wifi Device Name = " + wifi_portName);
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
  PlaybackChannelCountBox playbackChannelCountBox;

  PlaybackFileBox playbackFileBox;
  SDConverterBox sdConverterBox;

  BLEBox bleBox;
  DataLogBoxGanglion dataLogBoxGanglion;

  WifiBox wifiBox;
  InterfaceBoxCyton interfaceBoxCyton;
  InterfaceBoxGanglion interfaceBoxGanglion;
  SampleRateCytonBox sampleRateCytonBox;
  SampleRateGanglionBox sampleRateGanglionBox;
  LatencyCytonBox latencyCytonBox;
  LatencyGanglionBox latencyGanglionBox;
  WifiTransferProtcolCytonBox wifiTransferProtcolCytonBox;
  WifiTransferProtcolGanglionBox wifiTransferProtcolGanglionBox;

  SDBox sdBox;

  boolean drawStopInstructions;

  int globalPadding; //design feature: passed through to all box classes as the global spacing .. in pixels .. for all elements/subelements
  int globalBorder;

  boolean convertingSD = false;

  ControlPanel(OpenBCI_GUI mainClass) {

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
    interfaceBoxCyton = new InterfaceBoxCyton(x + w, dataSourceBox.y, w, h, globalPadding);
    interfaceBoxGanglion = new InterfaceBoxGanglion(x + w, dataSourceBox.y, w, h, globalPadding);

    serialBox = new SerialBox(x + w, interfaceBoxCyton.y + interfaceBoxCyton.h, w, h, globalPadding);
    wifiBox = new WifiBox(x + w, interfaceBoxCyton.y + interfaceBoxCyton.h, w, h, globalPadding);

    dataLogBox = new DataLogBox(x + w, (serialBox.y + serialBox.h), w, h, globalPadding);
    channelCountBox = new ChannelCountBox(x + w, (dataLogBox.y + dataLogBox.h), w, h, globalPadding);
    synthChannelCountBox = new SyntheticChannelCountBox(x + w, dataSourceBox.y, w, h, globalPadding);
    sdBox = new SDBox(x + w, (channelCountBox.y + channelCountBox.h), w, h, globalPadding);
    sampleRateCytonBox = new SampleRateCytonBox(x + w + x + w - 3, channelCountBox.y, w, h, globalPadding);
    latencyCytonBox = new LatencyCytonBox(x + w + x + w - 3, (sampleRateCytonBox.y + sampleRateCytonBox.h), w, h, globalPadding);
    wifiTransferProtcolCytonBox = new WifiTransferProtcolCytonBox(x + w + x + w - 3, (latencyCytonBox.y + latencyCytonBox.h), w, h, globalPadding);

    //boxes active when eegDataSource = Playback
    playbackChannelCountBox = new PlaybackChannelCountBox(x + w, dataSourceBox.y, w, h, globalPadding);
    playbackFileBox = new PlaybackFileBox(x + w, (playbackChannelCountBox.y + playbackChannelCountBox.h), w, h, globalPadding);
    sdConverterBox = new SDConverterBox(x + w, (playbackFileBox.y + playbackFileBox.h), w, h, globalPadding);

    rcBox = new RadioConfigBox(x+w, y, w, h, globalPadding);
    channelPopup = new ChannelPopup(x+w, y, w, h, globalPadding);
    pollPopup = new PollPopup(x+w,y,w,h,globalPadding);

    wcBox = new WifiConfigBox(x+w, y, w, h, globalPadding);

    initBox = new InitBox(x, (dataSourceBox.y + dataSourceBox.h), w, h, globalPadding);

    // Ganglion
    bleBox = new BLEBox(x + w, interfaceBoxGanglion.y + interfaceBoxGanglion.h, w, h, globalPadding);
    dataLogBoxGanglion = new DataLogBoxGanglion(x + w, (bleBox.y + bleBox.h), w, h, globalPadding);
    sampleRateGanglionBox = new SampleRateGanglionBox(x + w, (dataLogBoxGanglion.y + dataLogBoxGanglion.h), w, h, globalPadding);
    latencyGanglionBox = new LatencyGanglionBox(x + w, (sampleRateGanglionBox.y + sampleRateGanglionBox.h), w, h, globalPadding);
    wifiTransferProtcolGanglionBox = new WifiTransferProtcolGanglionBox(x + w, (latencyGanglionBox.y + latencyGanglionBox.h), w, h, globalPadding);
  }

  public void resetListItems(){
    serialList.activeItem = -1;
    bleList.activeItem = -1;
    wifiList.activeItem = -1;
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
    playbackChannelCountBox.update();
    sdBox.update();
    rcBox.update();
    wcBox.update();
    initBox.update();

    channelPopup.update();
    serialList.updateMenu();
    bleList.updateMenu();
    wifiList.updateMenu();
    dataLogBoxGanglion.update();
    latencyCytonBox.update();
    wifiTransferProtcolCytonBox.update();

    wifiBox.update();
    interfaceBoxCyton.update();
    interfaceBoxGanglion.update();
    latencyGanglionBox.update();
    wifiTransferProtcolGanglionBox.update();

    //SD File Conversion
    while (convertingSD == true) {
      convertSDFile();
    }

    if (isHubInitialized && isHubObjectInitialized) {
      if (ganglion.getInterface() == INTERFACE_HUB_BLE) {
        if (!calledForBLEList) {
          calledForBLEList = true;
          if (hub.isHubRunning()) {
            // Commented out because noble will auto scan
            // hub.searchDeviceStart();
          }
        }
      }

      if (ganglion.getInterface() == INTERFACE_HUB_WIFI || cyton.getInterface() == INTERFACE_HUB_WIFI) {
        if (!calledForWifiList) {
          calledForWifiList = true;
          if (hub.isHubRunning()) {
            hub.searchDeviceStart();
          }
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

      if (eegDataSource == DATASOURCE_CYTON) {	//when data source is from OpenBCI
        if (cyton.getInterface() == INTERFACE_NONE) {
          interfaceBoxCyton.draw();
        } else {
          interfaceBoxCyton.draw();
          if (cyton.getInterface() == INTERFACE_SERIAL) {
            serialBox.draw();
            cp5.get(MenuList.class, "serialList").setVisible(true);
            if (rcBox.isShowing) {
              rcBox.draw();
              if (channelPopup.wasClicked()) {
                channelPopup.draw();
                cp5Popup.get(MenuList.class, "channelList").setVisible(true);
                cp5Popup.get(MenuList.class, "pollList").setVisible(false);
                cp5.get(MenuList.class, "serialList").setVisible(true); //make sure the serialList menulist is visible
                cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
              } else if (pollPopup.wasClicked()) {
                pollPopup.draw();
                cp5Popup.get(MenuList.class, "pollList").setVisible(true);
                cp5Popup.get(MenuList.class, "channelList").setVisible(false);
                cp5.get(Textfield.class, "fileName").setVisible(true); //make sure the data file field is visible
                // cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
                cp5.get(MenuList.class, "serialList").setVisible(true); //make sure the serialList menulist is visible
                cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
              }
            }
          } else if (cyton.getInterface() == INTERFACE_HUB_WIFI) {
            wifiBox.draw();
            cp5.get(MenuList.class, "wifiList").setVisible(true);
            if(wcBox.isShowing){
              wcBox.draw();
            }
            sampleRateCytonBox.draw();
            latencyCytonBox.draw();
            wifiTransferProtcolCytonBox.draw();
          }
          // dataLogBox.y = serialBox.y + serialBox.h;
          dataLogBox.draw();
          channelCountBox.draw();
          sdBox.draw();
          cp5.get(Textfield.class, "fileName").setVisible(true); //make sure the data file field is visible
          cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is not visible
          // cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
          cp5.get(MenuList.class, "sdTimes").setVisible(true); //make sure the SD time record options menulist is visible
        }
      } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) { //when data source is from playback file
        playbackChannelCountBox.draw();
        playbackFileBox.draw();
        sdConverterBox.draw();

        //set other CP5 controllers invisible
        // cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
        // cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is visible
        cp5.get(MenuList.class, "serialList").setVisible(false);
        cp5.get(MenuList.class, "sdTimes").setVisible(false);
        cp5Popup.get(MenuList.class, "channelList").setVisible(false);
        cp5Popup.get(MenuList.class, "pollList").setVisible(false);

      } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  //synthetic
        //set other CP5 controllers invisible
        // hideAllBoxes();
        synthChannelCountBox.draw();
      } else if (eegDataSource == DATASOURCE_GANGLION) {
        if (ganglion.getInterface() == INTERFACE_NONE) {
          interfaceBoxGanglion.draw();
        } else {
          interfaceBoxGanglion.draw();
          if (ganglion.getInterface() == INTERFACE_HUB_BLE) {
            bleBox.draw();
            cp5.get(MenuList.class, "bleList").setVisible(true);
          } else if (ganglion.getInterface() == INTERFACE_HUB_WIFI) {
            wifiBox.draw();
            cp5.get(MenuList.class, "wifiList").setVisible(true);
            if(wcBox.isShowing){
              wcBox.draw();
            }
            latencyGanglionBox.draw();
            sampleRateGanglionBox.draw();
            wifiTransferProtcolGanglionBox.draw();
          }
          // dataLogBox.y = bleBox.y + bleBox.h;
          dataLogBoxGanglion.draw();
          cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
          cp5.get(Textfield.class, "fileNameGanglion").setVisible(true); //make sure the data file field is visible
        }
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

  public void hideRadioPopoutBox() {
    rcBox.isShowing = false;
    cp5Popup.hide(); // make sure to hide the controlP5 object
    cp5Popup.get(MenuList.class, "channelList").setVisible(false);
    cp5Popup.get(MenuList.class, "pollList").setVisible(false);
    // cp5Popup.hide(); // make sure to hide the controlP5 object
    popOutRadioConfigButton.setString(">");
    rcBox.print_onscreen("");
    if (board != null) {
      board.stop();
    }
    board = null;
  }

  public void hideWifiPopoutBox() {
    wcBox.isShowing = false;
    popOutWifiConfigButton.setString(">");
    wcBox.updateMessage("");
    if (hub.isPortOpen()) hub.closePort();
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
    //
    cp5.get(Textfield.class, "fileName").setVisible(false); //make sure the data file field is visible
    cp5.get(Textfield.class, "fileNameGanglion").setVisible(false); //make sure the data file field is visible
    cp5.get(MenuList.class, "serialList").setVisible(false);
    cp5.get(MenuList.class, "bleList").setVisible(false);
    cp5.get(MenuList.class, "sdTimes").setVisible(false);
    cp5.get(MenuList.class, "wifiList").setVisible(false);
    cp5Popup.get(MenuList.class, "channelList").setVisible(false);
    cp5Popup.get(MenuList.class, "pollList").setVisible(false);
  }

  //mouse pressed in control panel
  public void CPmousePressed() {
    // verbosePrint("CPmousePressed");

    if (initSystemButton.isMouseHere()) {
      initSystemButton.setIsActive(true);
      initSystemButton.wasPressed = true;
    }

    //only able to click buttons of control panel when system is not running
    if (systemMode != 10) {

      if ((eegDataSource == DATASOURCE_CYTON || eegDataSource == DATASOURCE_GANGLION) && (cyton.isWifi() || ganglion.isWifi())) {
        if(getIpAddress.isMouseHere()) {
          getIpAddress.setIsActive(true);
          getIpAddress.wasPressed = true;
        }

        if(getFirmwareVersion.isMouseHere()) {
          getFirmwareVersion.setIsActive(true);
          getFirmwareVersion.wasPressed = true;
        }

        if(getMacAddress.isMouseHere()) {
          getMacAddress.setIsActive(true);
          getMacAddress.wasPressed = true;
        }

        if(eraseCredentials.isMouseHere()) {
          eraseCredentials.setIsActive(true);
          eraseCredentials.wasPressed = true;
        }

        if(getTypeOfAttachedBoard.isMouseHere()) {
          getTypeOfAttachedBoard.setIsActive(true);
          getTypeOfAttachedBoard.wasPressed = true;
        }

        if (popOutWifiConfigButton.isMouseHere()){
          popOutWifiConfigButton.setIsActive(true);
          popOutWifiConfigButton.wasPressed = true;
        }
      }

      //active buttons during DATASOURCE_CYTON
      if (eegDataSource == DATASOURCE_CYTON) {
        if (cyton.isSerial()) {
          if (popOutRadioConfigButton.isMouseHere()){
            popOutRadioConfigButton.setIsActive(true);
            popOutRadioConfigButton.wasPressed = true;
          }
          if (refreshPort.isMouseHere()) {
            refreshPort.setIsActive(true);
            refreshPort.wasPressed = true;
          }
        }

        if (cyton.isWifi()) {
          if (refreshWifi.isMouseHere()) {
            refreshWifi.setIsActive(true);
            refreshWifi.wasPressed = true;
          }
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



        if (protocolWifiCyton.isMouseHere()) {
          protocolWifiCyton.setIsActive(true);
          protocolWifiCyton.wasPressed = true;
          protocolWifiCyton.color_notPressed = isSelected_color;
          protocolSerialCyton.color_notPressed = autoFileName.color_notPressed;
        }

        if (protocolSerialCyton.isMouseHere()) {
          protocolSerialCyton.setIsActive(true);
          protocolSerialCyton.wasPressed = true;
          protocolWifiCyton.color_notPressed = autoFileName.color_notPressed;
          protocolSerialCyton.color_notPressed = isSelected_color;
        }

        if (autoscan.isMouseHere()){
          autoscan.setIsActive(true);
          autoscan.wasPressed = true;
        }

        if (systemStatus.isMouseHere()){
          systemStatus.setIsActive(true);
          systemStatus.wasPressed = true;
        }

        if (sampleRate250.isMouseHere()) {
          sampleRate250.setIsActive(true);
          sampleRate250.wasPressed = true;
          sampleRate250.color_notPressed = isSelected_color;
          sampleRate500.color_notPressed = autoFileName.color_notPressed;
          sampleRate1000.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (sampleRate500.isMouseHere()) {
          sampleRate500.setIsActive(true);
          sampleRate500.wasPressed = true;
          sampleRate500.color_notPressed = isSelected_color;
          sampleRate250.color_notPressed = autoFileName.color_notPressed;
          sampleRate1000.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (sampleRate1000.isMouseHere()) {
          sampleRate1000.setIsActive(true);
          sampleRate1000.wasPressed = true;
          sampleRate1000.color_notPressed = isSelected_color;
          sampleRate250.color_notPressed = autoFileName.color_notPressed; //default color of button
          sampleRate500.color_notPressed = autoFileName.color_notPressed;
        }

        if (latencyCyton5ms.isMouseHere()) {
          latencyCyton5ms.setIsActive(true);
          latencyCyton5ms.wasPressed = true;
          latencyCyton5ms.color_notPressed = isSelected_color;
          latencyCyton10ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyCyton20ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (latencyCyton10ms.isMouseHere()) {
          latencyCyton10ms.setIsActive(true);
          latencyCyton10ms.wasPressed = true;
          latencyCyton10ms.color_notPressed = isSelected_color;
          latencyCyton5ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyCyton20ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (latencyCyton20ms.isMouseHere()) {
          latencyCyton20ms.setIsActive(true);
          latencyCyton20ms.wasPressed = true;
          latencyCyton20ms.color_notPressed = isSelected_color;
          latencyCyton5ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyCyton10ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolCytonTCP.isMouseHere()) {
          wifiInternetProtocolCytonTCP.setIsActive(true);
          wifiInternetProtocolCytonTCP.wasPressed = true;
          wifiInternetProtocolCytonTCP.color_notPressed = isSelected_color;
          wifiInternetProtocolCytonUDP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolCytonUDPBurst.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolCytonUDP.isMouseHere()) {
          wifiInternetProtocolCytonUDP.setIsActive(true);
          wifiInternetProtocolCytonUDP.wasPressed = true;
          wifiInternetProtocolCytonUDP.color_notPressed = isSelected_color;
          wifiInternetProtocolCytonTCP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolCytonUDPBurst.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolCytonUDPBurst.isMouseHere()) {
          wifiInternetProtocolCytonUDPBurst.setIsActive(true);
          wifiInternetProtocolCytonUDPBurst.wasPressed = true;
          wifiInternetProtocolCytonUDPBurst.color_notPressed = isSelected_color;
          wifiInternetProtocolCytonTCP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolCytonUDP.color_notPressed = autoFileName.color_notPressed; //default color of button
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

        if (ganglion.isWifi()) {
          if (refreshWifi.isMouseHere()) {
            refreshWifi.setIsActive(true);
            refreshWifi.wasPressed = true;
          }
        } else {
          if (refreshBLE.isMouseHere()) {
            refreshBLE.setIsActive(true);
            refreshBLE.wasPressed = true;
          }
        }

        if (protocolBLEGanglion.isMouseHere()) {
          protocolBLEGanglion.setIsActive(true);
          protocolBLEGanglion.wasPressed = true;
          protocolBLEGanglion.color_notPressed = isSelected_color;
          protocolWifiGanglion.color_notPressed = autoFileName.color_notPressed;
        }

        if (protocolWifiGanglion.isMouseHere()) {
          protocolWifiGanglion.setIsActive(true);
          protocolWifiGanglion.wasPressed = true;
          protocolWifiGanglion.color_notPressed = isSelected_color;
          protocolBLEGanglion.color_notPressed = autoFileName.color_notPressed;
        }

        if (sampleRate200.isMouseHere()) {
          sampleRate200.setIsActive(true);
          sampleRate200.wasPressed = true;
          sampleRate200.color_notPressed = isSelected_color;
          sampleRate1600.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (sampleRate1600.isMouseHere()) {
          sampleRate1600.setIsActive(true);
          sampleRate1600.wasPressed = true;
          sampleRate1600.color_notPressed = isSelected_color;
          sampleRate200.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (latencyGanglion5ms.isMouseHere()) {
          latencyGanglion5ms.setIsActive(true);
          latencyGanglion5ms.wasPressed = true;
          latencyGanglion5ms.color_notPressed = isSelected_color;
          latencyGanglion10ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyGanglion20ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (latencyGanglion10ms.isMouseHere()) {
          latencyGanglion10ms.setIsActive(true);
          latencyGanglion10ms.wasPressed = true;
          latencyGanglion10ms.color_notPressed = isSelected_color;
          latencyGanglion5ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyGanglion20ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (latencyGanglion20ms.isMouseHere()) {
          latencyGanglion20ms.setIsActive(true);
          latencyGanglion20ms.wasPressed = true;
          latencyGanglion20ms.color_notPressed = isSelected_color;
          latencyGanglion5ms.color_notPressed = autoFileName.color_notPressed; //default color of button
          latencyGanglion10ms.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolGanglionTCP.isMouseHere()) {
          wifiInternetProtocolGanglionTCP.setIsActive(true);
          wifiInternetProtocolGanglionTCP.wasPressed = true;
          wifiInternetProtocolGanglionTCP.color_notPressed = isSelected_color;
          wifiInternetProtocolGanglionUDP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolGanglionUDPBurst.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolGanglionUDP.isMouseHere()) {
          wifiInternetProtocolGanglionUDP.setIsActive(true);
          wifiInternetProtocolGanglionUDP.wasPressed = true;
          wifiInternetProtocolGanglionUDP.color_notPressed = isSelected_color;
          wifiInternetProtocolGanglionTCP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolGanglionUDPBurst.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (wifiInternetProtocolGanglionUDPBurst.isMouseHere()) {
          wifiInternetProtocolGanglionUDPBurst.setIsActive(true);
          wifiInternetProtocolGanglionUDPBurst.wasPressed = true;
          wifiInternetProtocolGanglionUDPBurst.color_notPressed = isSelected_color;
          wifiInternetProtocolGanglionTCP.color_notPressed = autoFileName.color_notPressed; //default color of button
          wifiInternetProtocolGanglionUDP.color_notPressed = autoFileName.color_notPressed; //default color of button
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

        if (playbackChanButton4.isMouseHere()) {
          playbackChanButton4.setIsActive(true);
          playbackChanButton4.wasPressed = true;
          playbackChanButton4.color_notPressed = isSelected_color;
          playbackChanButton8.color_notPressed = autoFileName.color_notPressed; //default color of button
          playbackChanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (playbackChanButton8.isMouseHere()) {
          playbackChanButton8.setIsActive(true);
          playbackChanButton8.wasPressed = true;
          playbackChanButton8.color_notPressed = isSelected_color;
          playbackChanButton4.color_notPressed = autoFileName.color_notPressed; //default color of button
          playbackChanButton16.color_notPressed = autoFileName.color_notPressed; //default color of button
        }

        if (playbackChanButton16.isMouseHere()) {
          playbackChanButton16.setIsActive(true);
          playbackChanButton16.wasPressed = true;
          playbackChanButton16.color_notPressed = isSelected_color;
          playbackChanButton4.color_notPressed = autoFileName.color_notPressed; //default color of button
          playbackChanButton8.color_notPressed = autoFileName.color_notPressed; //default color of button
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
    if(popOutRadioConfigButton.isMouseHere() && popOutRadioConfigButton.wasPressed){
      popOutRadioConfigButton.wasPressed = false;
      popOutRadioConfigButton.setIsActive(false);
      if (cyton.isSerial()) {
        if(rcBox.isShowing){
          hideRadioPopoutBox();
        }
        else{
          rcBox.isShowing = true;
          popOutRadioConfigButton.setString("<");
        }
      }
    }

    if (rcBox.isShowing) {
      if(getChannel.isMouseHere() && getChannel.wasPressed){
        // if(board != null) // Radios_Config will handle creating the serial port JAM 1/2017
        get_channel(rcBox);
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

      if(autoscan.isMouseHere() && autoscan.wasPressed){
        autoscan.wasPressed = false;
        autoscan.setIsActive(false);
        scan_channels(rcBox);
      }

      if(systemStatus.isMouseHere() && systemStatus.wasPressed){
        system_status(rcBox);
        systemStatus.setIsActive(false);
        systemStatus.wasPressed = false;
      }
    }

    if(popOutWifiConfigButton.isMouseHere() && popOutWifiConfigButton.wasPressed){
      popOutWifiConfigButton.wasPressed = false;
      popOutWifiConfigButton.setIsActive(false);
      if (cyton.isWifi() || ganglion.isWifi()) {
        if(wcBox.isShowing){
          hideWifiPopoutBox();
        } else {
          if (wifi_portName == "N/A") {
            output("Please select a WiFi Shield first. Can't see your WiFi Shield? Learn how at docs.openbci.com/Tutorials/03-Wifi_Getting_Started_Guide");
          } else {
            output("Attempting to connect to WiFi Shield named " + wifi_portName);
            hub.examineWifi(wifi_portName);
            wcBox.isShowing = true;
            popOutWifiConfigButton.setString("<");
          }
        }
      }
    }

    if (wcBox.isShowing) {
      if(getIpAddress.isMouseHere() && getIpAddress.wasPressed){
        hub.getWifiInfo(hub.TCP_WIFI_GET_IP_ADDRESS);
        getIpAddress.wasPressed = false;
        getIpAddress.setIsActive(false);
      }

      if(getFirmwareVersion.isMouseHere() && getFirmwareVersion.wasPressed){
        hub.getWifiInfo(hub.TCP_WIFI_GET_FIRMWARE_VERSION);
        getFirmwareVersion.wasPressed = false;
        getFirmwareVersion.setIsActive(false);
      }

      if(getMacAddress.isMouseHere() && getMacAddress.wasPressed){
        hub.getWifiInfo(hub.TCP_WIFI_GET_MAC_ADDRESS);
        getMacAddress.wasPressed = false;
        getMacAddress.setIsActive(false);
      }

      if(eraseCredentials.isMouseHere() && eraseCredentials.wasPressed){
        hub.getWifiInfo(hub.TCP_WIFI_ERASE_CREDENTIALS);
        eraseCredentials.wasPressed=false;
        eraseCredentials.setIsActive(false);
      }

      if(getTypeOfAttachedBoard.isMouseHere() && getTypeOfAttachedBoard.wasPressed){
        // Wifi_Config will handle creating the connection
        hub.getWifiInfo(hub.TCP_WIFI_GET_TYPE_OF_ATTACHED_BOARD);
        getTypeOfAttachedBoard.wasPressed=false;
        getTypeOfAttachedBoard.setIsActive(false);
      }
    }

    if (initSystemButton.isMouseHere() && initSystemButton.wasPressed) {
      if (rcBox.isShowing) {
        hideRadioPopoutBox();
      }
      if (wcBox.isShowing) {
        hideWifiPopoutBox();
      }
      //if system is not active ... initate system and flip button state
      initButtonPressed();
      //cursor(ARROW); //this this back to ARROW
    }

    //open or close serial port if serial port button is pressed (left button in serial widget)
    if (refreshPort.isMouseHere() && refreshPort.wasPressed) {
      output("Serial/COM List Refreshed");
      refreshPortList();
    }

    if (refreshBLE.isMouseHere() && refreshBLE.wasPressed) {
      if (isHubObjectInitialized) {
        output("BLE Devices Refreshing");
        bleList.items.clear();
        hub.searchDeviceStart();
      } else {
        output("Please wait till BLE is fully initalized");
      }
    }

    if (refreshWifi.isMouseHere() && refreshWifi.wasPressed) {
      if (isHubObjectInitialized) {
        output("Wifi Devices Refreshing");
        wifiList.items.clear();
        hub.searchDeviceStart();
      } else {
        output("Please wait till hub is fully initalized");
      }
    }

    if (protocolBLEGanglion.isMouseHere() && protocolBLEGanglion.wasPressed) {
      wifiList.items.clear();
      bleList.items.clear();
      controlPanel.hideAllBoxes();
      if (isHubObjectInitialized) {
        output("Protocol BLE Selected for Ganglion");
        if (hub.isPortOpen()) hub.closePort();
        ganglion.setInterface(INTERFACE_HUB_BLE);
      } else {
        outputWarn("Please wait till hub is fully initalized");
      }
    }

    if (protocolWifiGanglion.isMouseHere() && protocolWifiGanglion.wasPressed) {
      println("protocolWifiGanglion");
      wifiList.items.clear();
      bleList.items.clear();
      controlPanel.hideAllBoxes();
      println("isHubObjectInitialized: " + (isHubObjectInitialized ? "true" : "else"));
      if (isHubObjectInitialized) {
        output("Protocol Wifi Selected for Ganglion");
        if (hub.isPortOpen()) hub.closePort();
        ganglion.setInterface(INTERFACE_HUB_WIFI);
      } else {
        output("Please wait till hub is fully initalized");
      }
    }

    if (protocolSerialCyton.isMouseHere() && protocolSerialCyton.wasPressed) {
      wifiList.items.clear();
      bleList.items.clear();
      controlPanel.hideAllBoxes();
      if (isHubObjectInitialized) {
        output("Protocol Serial Selected for Cyton");
        if (hub.isPortOpen()) hub.closePort();
        cyton.setInterface(INTERFACE_SERIAL);
      } else {
        output("Please wait till hub is fully initalized");
      }
    }

    if (protocolWifiCyton.isMouseHere() && protocolWifiCyton.wasPressed) {
      wifiList.items.clear();
      bleList.items.clear();
      controlPanel.hideAllBoxes();
      if (isHubObjectInitialized) {
        output("Protocol Wifi Selected for Cyton");
        if (hub.isPortOpen()) hub.closePort();
        cyton.setInterface(INTERFACE_HUB_WIFI);
      } else {
        output("Please wait till hub is fully initalized");
      }
    }

    // if (protocolBLEGanglion.isMouseHere()) {
    //   protocolBLEGanglion.setIsActive(true);
    //   protocolBLEGanglion.wasPressed = true;
    // }
    //
    // if (protocolWifiGanglion.isMouseHere()) {
    //   protocolWifiGanglion.setIsActive(true);
    //   protocolWifiGanglion.wasPressed = true;
    // }

    //open or close serial port if serial port button is pressed (left button in serial widget)
    if (autoFileName.isMouseHere() && autoFileName.wasPressed) {
      output("Autogenerated Cyton \"File Name\" based on current date/time");
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
      output("Autogenerated Ganglion \"File Name\" based on current date/time");
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

    if (sampleRate200.isMouseHere() && sampleRate200.wasPressed) {
      ganglion.setSampleRate(200);
    }

    if (sampleRate1600.isMouseHere() && sampleRate1600.wasPressed) {
      ganglion.setSampleRate(1600);
    }

    if (sampleRate250.isMouseHere() && sampleRate250.wasPressed) {
      cyton.setSampleRate(250);
    }

    if (sampleRate500.isMouseHere() && sampleRate500.wasPressed) {
      cyton.setSampleRate(500);
    }

    if (sampleRate1000.isMouseHere() && sampleRate1000.wasPressed) {
      cyton.setSampleRate(1000);
    }

    if (playbackChanButton4.isMouseHere() && playbackChanButton4.wasPressed) {
      updateToNChan(4);
    }

    if (playbackChanButton8.isMouseHere() && playbackChanButton8.wasPressed) {
      updateToNChan(8);
    }

    if (playbackChanButton16.isMouseHere() && playbackChanButton16.wasPressed) {
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

    if (latencyCyton5ms.isMouseHere() && latencyCyton5ms.wasPressed) {
      hub.setLatency(hub.LATENCY_5_MS);
    }

    if (latencyCyton10ms.isMouseHere() && latencyCyton10ms.wasPressed) {
      hub.setLatency(hub.LATENCY_10_MS);
    }

    if (latencyCyton20ms.isMouseHere() && latencyCyton20ms.wasPressed) {
      hub.setLatency(hub.LATENCY_20_MS);
    }

    if (latencyGanglion5ms.isMouseHere() && latencyGanglion5ms.wasPressed) {
      hub.setLatency(hub.LATENCY_5_MS);
    }

    if (latencyGanglion10ms.isMouseHere() && latencyGanglion10ms.wasPressed) {
      hub.setLatency(hub.LATENCY_10_MS);
    }

    if (latencyGanglion20ms.isMouseHere() && latencyGanglion20ms.wasPressed) {
      hub.setLatency(hub.LATENCY_20_MS);
    }

    if (wifiInternetProtocolCytonTCP.isMouseHere() && wifiInternetProtocolCytonTCP.wasPressed) {
      hub.setWifiInternetProtocol(hub.TCP);
    }

    if (wifiInternetProtocolCytonUDP.isMouseHere() && wifiInternetProtocolCytonUDP.wasPressed) {
      hub.setWifiInternetProtocol(hub.UDP);
    }

    if (wifiInternetProtocolCytonUDPBurst.isMouseHere() && wifiInternetProtocolCytonUDPBurst.wasPressed) {
      hub.setWifiInternetProtocol(hub.UDP_BURST);
    }

    if (wifiInternetProtocolGanglionTCP.isMouseHere() && wifiInternetProtocolGanglionTCP.wasPressed) {
      hub.setWifiInternetProtocol(hub.TCP);
    }

    if (wifiInternetProtocolGanglionUDP.isMouseHere() && wifiInternetProtocolGanglionUDP.wasPressed) {
      hub.setWifiInternetProtocol(hub.UDP);
    }

    if (wifiInternetProtocolGanglionUDPBurst.isMouseHere() && wifiInternetProtocolGanglionUDPBurst.wasPressed) {
      hub.setWifiInternetProtocol(hub.UDP_BURST);
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
    refreshWifi.setIsActive(false);
    refreshWifi.wasPressed = false;
    protocolBLEGanglion.setIsActive(false);
    protocolBLEGanglion.wasPressed = false;
    protocolWifiGanglion.setIsActive(false);
    protocolWifiGanglion.wasPressed = false;
    protocolSerialCyton.setIsActive(false);
    protocolSerialCyton.wasPressed = false;
    protocolWifiCyton.setIsActive(false);
    protocolWifiCyton.wasPressed = false;
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
    sampleRate200.setIsActive(false);
    sampleRate200.wasPressed = false;
    sampleRate1600.setIsActive(false);
    sampleRate1600.wasPressed = false;
    sampleRate250.setIsActive(false);
    sampleRate250.wasPressed = false;
    sampleRate500.setIsActive(false);
    sampleRate500.wasPressed = false;
    sampleRate1000.setIsActive(false);
    sampleRate1000.wasPressed = false;
    latencyCyton5ms.setIsActive(false);
    latencyCyton5ms.wasPressed = false;
    latencyCyton10ms.setIsActive(false);
    latencyCyton10ms.wasPressed = false;
    latencyCyton20ms.setIsActive(false);
    latencyCyton20ms.wasPressed = false;
    latencyGanglion5ms.setIsActive(false);
    latencyGanglion5ms.wasPressed = false;
    latencyGanglion10ms.setIsActive(false);
    latencyGanglion10ms.wasPressed = false;
    latencyGanglion20ms.setIsActive(false);
    latencyGanglion20ms.wasPressed = false;
    wifiInternetProtocolCytonTCP.setIsActive(false);
    wifiInternetProtocolCytonTCP.wasPressed = false;
    wifiInternetProtocolCytonUDP.setIsActive(false);
    wifiInternetProtocolCytonUDP.wasPressed = false;
    wifiInternetProtocolCytonUDPBurst.setIsActive(false);
    wifiInternetProtocolCytonUDPBurst.wasPressed = false;
    wifiInternetProtocolGanglionTCP.setIsActive(false);
    wifiInternetProtocolGanglionTCP.wasPressed = false;
    wifiInternetProtocolGanglionUDP.setIsActive(false);
    wifiInternetProtocolGanglionUDP.wasPressed = false;
    wifiInternetProtocolGanglionUDPBurst.setIsActive(false);
    wifiInternetProtocolGanglionUDPBurst.wasPressed = false;
    synthChanButton4.setIsActive(false);
    synthChanButton4.wasPressed = false;
    synthChanButton8.setIsActive(false);
    synthChanButton8.wasPressed = false;
    synthChanButton16.setIsActive(false);
    synthChanButton16.wasPressed = false;
    playbackChanButton4.setIsActive(false);
    playbackChanButton4.wasPressed = false;
    playbackChanButton8.setIsActive(false);
    playbackChanButton8.wasPressed = false;
    playbackChanButton16.setIsActive(false);
    playbackChanButton16.wasPressed = false;
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
      if ((eegDataSource == DATASOURCE_CYTON && cyton.getInterface() == INTERFACE_NONE) || (eegDataSource == DATASOURCE_GANGLION && ganglion.getInterface() == INTERFACE_NONE)) {
        output("No Transfer Protocol selected. Please select your Transfer Protocol and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_CYTON && cyton.getInterface() == INTERFACE_SERIAL && openBCI_portName == "N/A") { //if data source == normal && if no serial port selected OR no SD setting selected
        output("No Serial/COM port selected. Please select your Serial/COM port and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_CYTON && cyton.getInterface() == INTERFACE_HUB_WIFI && wifi_portName == "N/A") {
        output("No Wifi Shield selected. Please select your Wifi Shield and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_PLAYBACKFILE && playbackData_fname == "N/A") { //if data source == playback && playback file == 'N/A'
        output("No playback file selected. Please select a playback file and retry system initiation.");        // tell user that they need to select a file before the system can be started
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_GANGLION && ganglion.getInterface() == INTERFACE_HUB_BLE && ganglion_portName == "N/A") {
        output("No BLE device selected. Please select your Ganglion device and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
      } else if (eegDataSource == DATASOURCE_GANGLION && ganglion.getInterface() == INTERFACE_HUB_WIFI && wifi_portName == "N/A") {
        output("No Wifi Shield selected. Please select your Wifi Shield and retry system initiation.");
        initSystemButton.wasPressed = false;
        initSystemButton.setIsActive(false);
        return;
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
        if (eegDataSource == DATASOURCE_CYTON) {
          verbosePrint("ControlPanel \u2014 port is open: " + cyton.isPortOpen());
          if (cyton.isPortOpen() == true) {
            cyton.closePort();
          }
        } else if(eegDataSource == DATASOURCE_GANGLION){
          verbosePrint("ControlPanel \u2014 port is open: " + ganglion.isPortOpen());
          if (ganglion.isPortOpen()) {
            ganglion.closePort();
          }
        }
        if(eegDataSource == DATASOURCE_GANGLION){
          fileName = cp5.get(Textfield.class, "fileNameGanglion").getText(); // store the current text field value of "File Name" to be passed along to dataFiles
        } else if(eegDataSource == DATASOURCE_CYTON){
          fileName = cp5.get(Textfield.class, "fileName").getText(); // store the current text field value of "File Name" to be passed along to dataFiles
        }
        midInit = true;
        println("initSystem yoo");
        initSystem(); //calls the initSystem() funciton of the OpenBCI_GUI.pde file
      }
    }

    //if system is already active ... stop system and flip button state back
    else {
      output("Learn how to use this application and more at docs.openbci.com");
      initSystemButton.setString("START SYSTEM");
      cp5.get(Textfield.class, "fileName").setText(getDateString()); //creates new data file name so that you don't accidentally overwrite the old one
      cp5.get(Textfield.class, "fileNameGanglion").setText(getDateString()); //creates new data file name so that you don't accidentally overwrite the old one
      haltSystem();
    }
}

public void updateToNChan(int _nchan) {
  nchan = _nchan;
  fftBuff = new FFT[nchan];  //reinitialize the FFT buffer
  yLittleBuff_uV = new float[nchan][nPointsPerUpdate];
  output("Channel count set to " + str(nchan));
  println("channel count set to " + str(nchan));
  hub.initDataPackets(_nchan, 3);
  ganglion.initDataPackets(_nchan, 3);
  cyton.initDataPackets(_nchan, 3);
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
    h = 140 + _padding;
    padding = _padding;

    // autoconnect = new Button(x + padding, y + padding*3 + 4, w - padding*2, 24, "AUTOCONNECT AND START SYSTEM", fontInfo.buttonLabel_size);
    refreshPort = new Button (x + padding, y + padding*4 + 72 + 8, w - padding*2, 24, "REFRESH LIST", fontInfo.buttonLabel_size);
    popOutRadioConfigButton = new Button(x+padding + (w-padding*4), y + padding, 20,20,">",fontInfo.buttonLabel_size);

    serialList = new MenuList(cp5, "serialList", w - padding*2, 72, p4);
    // println(w-padding*2);
    serialList.setPosition(x + padding, y + padding*3 + 8);
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
    // autoconnect.draw();
    if (cyton.isSerial()) {
      popOutRadioConfigButton.draw();
    }
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
    h = 140 + _padding;
    padding = _padding;

    refreshBLE = new Button (x + padding, y + padding*4 + 72 + 8, w - padding*5, 24, "START SEARCH", fontInfo.buttonLabel_size);
    bleList = new MenuList(cp5, "bleList", w - padding*2, 72, p4);
    // println(w-padding*2);
    bleList.setPosition(x + padding, y + padding*3 + 8);
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

    if(isHubInitialized && isHubObjectInitialized && ganglion.isBLE() && hub.isSearching()){
      image(loadingGIF_blue, w + 225,  y + padding*4 + 72 + 10, 20, 20);
      refreshBLE.setString("SEARCHING...");
    } else {
      refreshBLE.setString("START SEARCH");
    }
  }

  public void refreshBLEList() {
    bleList.items.clear();
    for (int i = 0; i < hub.deviceList.length; i++) {
      String tempPort = hub.deviceList[i];
      bleList.addItem(makeItem(tempPort));
    }
    bleList.updateMenu();
  }
};

class WifiBox {
  int x, y, w, h, padding; //size and position
  //connect/disconnect button
  //Refresh list button
  //String port status;

  WifiBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 140 + _padding;
    padding = _padding;

    refreshWifi = new Button (x + padding, y + padding*4 + 72 + 8, w - padding*5, 24, "START SEARCH", fontInfo.buttonLabel_size);
    wifiList = new MenuList(cp5, "wifiList", w - padding*2, 72, p4);
    popOutWifiConfigButton = new Button(x+padding + (w-padding*4), y + padding, 20,20,">",fontInfo.buttonLabel_size);

    // println(w-padding*2);
    wifiList.setPosition(x + padding, y + padding*3 + 8);
    // Call to update the list
    // ganglion.getBLEDevices();
  }

  public void update() {
    // Quick check to see if there are just more or less devices in general

  }

  public void updateListPosition(){
    wifiList.setPosition(x + padding, y + padding * 3);
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
    text("WIFI SHIELDS", x + padding, y + padding);
    popStyle();

    refreshWifi.draw();
    popOutWifiConfigButton.draw();

    if(isHubInitialized && isHubObjectInitialized && (ganglion.isWifi() || cyton.isWifi()) && hub.isSearching()){
      image(loadingGIF_blue, w + 225,  y + padding*4 + 72 + 10, 20, 20);
      refreshWifi.setString("SEARCHING...");
    } else {
      refreshWifi.setString("START SEARCH");
      pushStyle();
      fill(0xff999999);
      ellipseMode(CENTER);
      ellipse(w + 225 + 10,  y + padding*4 + 72 + 10 + 10, 12, 12);
      popStyle();
    }
  }

  public void refreshWifiList() {
    println("refreshWifiList");
    wifiList.items.clear();
    if (hub.deviceList != null) {
      for (int i = 0; i < hub.deviceList.length; i++) {
        String tempPort = hub.deviceList[i];
        wifiList.addItem(makeItem(tempPort));
      }
    }
    wifiList.updateMenu();
  }
};

class InterfaceBoxCyton {
  int x, y, w, h, padding; //size and position

  InterfaceBoxCyton(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = (24 + _padding) * 3;
    padding = _padding;

    protocolSerialCyton = new Button (x + padding, y + padding * 3, w - padding * 2, 24, "Serial (from Dongle)", fontInfo.buttonLabel_size);
    protocolWifiCyton = new Button (x + padding, y + padding * 4 + 24, w - padding * 2, 24, "Wifi (from Wifi Shield)", fontInfo.buttonLabel_size);
  }

  public void update() {}

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("PICK TRANSFER PROTOCOL", x + padding, y + padding);
    popStyle();

    protocolSerialCyton.draw();
    protocolWifiCyton.draw();
  }
};

class InterfaceBoxGanglion {
  int x, y, w, h, padding; //size and position

  InterfaceBoxGanglion(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = (24 + _padding) * 3;
    padding = _padding;

    protocolBLEGanglion = new Button (x + padding, y + padding * 3, w - padding * 2, 24, "BLE (on Win from Dongle)", fontInfo.buttonLabel_size);
    protocolWifiGanglion = new Button (x + padding, y + padding * 4 + 24, w - padding * 2, 24, "Wifi (from Wifi Shield)", fontInfo.buttonLabel_size);
  }

  public void update() {}

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("PICK TRANSFER PROTOCOL", x + padding, y + padding);
    popStyle();

    protocolBLEGanglion.draw();
    protocolWifiGanglion.draw();
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

class SampleRateGanglionBox {
  int x, y, w, h, padding; //size and position

  boolean isSystemInitialized;
  // button for init/halt system

  SampleRateGanglionBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    sampleRate200 = new Button (x + padding, y + padding*2 + 18, (w-padding*3)/2, 24, "200Hz", fontInfo.buttonLabel_size);
    sampleRate1600 = new Button (x + padding*2 + (w-padding*3)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "1600Hz", fontInfo.buttonLabel_size);
    sampleRate1600.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("SAMPLE RATE ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  " + str((int)ganglion.getSampleRate()) + "Hz", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    sampleRate200.draw();
    sampleRate1600.draw();
  }
};

class SampleRateCytonBox {
  int x, y, w, h, padding; //size and position

  boolean isSystemInitialized;
  // button for init/halt system

  SampleRateCytonBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    sampleRate250 = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "250Hz", fontInfo.buttonLabel_size);
    sampleRate500 = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "500Hz", fontInfo.buttonLabel_size);
    sampleRate1000 = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "1000Hz", fontInfo.buttonLabel_size);
    sampleRate1000.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("SAMPLE RATE ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  " + str((int)cyton.getSampleRate()) + "Hz", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    sampleRate250.draw();
    sampleRate500.draw();
    sampleRate1000.draw();
  }
};

class LatencyGanglionBox {
  int x, y, w, h, padding; //size and position

  LatencyGanglionBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    latencyGanglion5ms = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "5ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_5_MS) latencyGanglion5ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
    latencyGanglion10ms = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "10ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_10_MS) latencyGanglion10ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
    latencyGanglion20ms = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "20ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_20_MS) latencyGanglion20ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("LATENCY ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  " + str(hub.getLatency()/1000) + "ms", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    latencyGanglion5ms.draw();
    latencyGanglion10ms.draw();
    latencyGanglion20ms.draw();
  }
};

class LatencyCytonBox {
  int x, y, w, h, padding; //size and position

  LatencyCytonBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    latencyCyton5ms = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "5ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_5_MS) latencyCyton5ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
    latencyCyton10ms = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "10ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_10_MS) latencyCyton10ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
    latencyCyton20ms = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "20ms", fontInfo.buttonLabel_size);
    if (hub.getLatency() == hub.LATENCY_20_MS) latencyCyton20ms.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("LATENCY ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("  " + str(hub.getLatency()/1000) + "ms", x + padding + 142, y + padding); // print the channel count in green next to the box title
    popStyle();

    latencyCyton5ms.draw();
    latencyCyton10ms.draw();
    latencyCyton20ms.draw();
  }
};

class WifiTransferProtcolGanglionBox {
  int x, y, w, h, padding; //size and position

  WifiTransferProtcolGanglionBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    wifiInternetProtocolGanglionTCP = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "TCP", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.TCP)) wifiInternetProtocolGanglionTCP.color_notPressed = isSelected_color; //make it appear like this one is already selected
    wifiInternetProtocolGanglionUDP = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "UDP", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.UDP)) wifiInternetProtocolGanglionUDP.color_notPressed = isSelected_color; //make it appear like this one is already selected
    wifiInternetProtocolGanglionUDPBurst = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "UDPx3", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.UDP_BURST)) wifiInternetProtocolGanglionUDPBurst.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("WiFi Transfer Protocol ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    String dispText;
    if (hub.getWifiInternetProtocol().equals(hub.TCP)) {
      dispText = "TCP";
    } else if (hub.getWifiInternetProtocol().equals(hub.UDP)) {
      dispText = "UDP";
    } else {
      dispText = "UDPx3";
    }
    text(dispText, x + padding + 184, y + padding); // print the channel count in green next to the box title
    popStyle();

    wifiInternetProtocolGanglionTCP.draw();
    wifiInternetProtocolGanglionUDP.draw();
    wifiInternetProtocolGanglionUDPBurst.draw();
  }
};

class WifiTransferProtcolCytonBox {
  int x, y, w, h, padding; //size and position

  WifiTransferProtcolCytonBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    wifiInternetProtocolCytonTCP = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "TCP", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.TCP)) wifiInternetProtocolCytonTCP.color_notPressed = isSelected_color; //make it appear like this one is already selected
    wifiInternetProtocolCytonUDP = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "UDP", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.UDP)) wifiInternetProtocolCytonUDP.color_notPressed = isSelected_color; //make it appear like this one is already selected
    wifiInternetProtocolCytonUDPBurst = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "UDPx3", fontInfo.buttonLabel_size);
    if (hub.getWifiInternetProtocol().equals(hub.UDP_BURST)) wifiInternetProtocolCytonUDPBurst.color_notPressed = isSelected_color; //make it appear like this one is already selected
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
    text("WiFi Transfer Protocol ", x + padding, y + padding);
    fill(bgColor); //set color to green
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    String dispText;
    if (hub.getWifiInternetProtocol().equals(hub.TCP)) {
      dispText = "TCP";
    } else if (hub.getWifiInternetProtocol().equals(hub.UDP)) {
      dispText = "UDP";
    } else {
      dispText = "UDPx3";
    }
    text(dispText, x + padding + 184, y + padding); // print the channel count in green next to the box title
    popStyle();

    wifiInternetProtocolCytonTCP.draw();
    wifiInternetProtocolCytonUDP.draw();
    wifiInternetProtocolCytonUDPBurst.draw();
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

class PlaybackChannelCountBox {
  int x, y, w, h, padding; //size and position

  boolean isSystemInitialized;
  // button for init/halt system

  PlaybackChannelCountBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x;
    y = _y;
    w = _w;
    h = 73;
    padding = _padding;

    playbackChanButton4 = new Button (x + padding, y + padding*2 + 18, (w-padding*4)/3, 24, "4 chan", fontInfo.buttonLabel_size);
    if (nchan == 4) playbackChanButton4.color_notPressed = isSelected_color; //make it appear like this one is already selected
    playbackChanButton8 = new Button (x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24, "8 chan", fontInfo.buttonLabel_size);
    if (nchan == 8) playbackChanButton8.color_notPressed = isSelected_color; //make it appear like this one is already selected
    playbackChanButton16 = new Button (x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24, "16 chan", fontInfo.buttonLabel_size);
    if (nchan == 16) playbackChanButton16.color_notPressed = isSelected_color; //make it appear like this one is already selected
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

    playbackChanButton4.draw();
    playbackChanButton8.draw();
    playbackChanButton16.draw();
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

    sdTimes.activeItem = sdSetting; //added to indicate default choice (sdSetting is in OpenBCI_GUI)
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
    systemStatus = new Button(x + 2*padding + (w-padding*3)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "STATUS", fontInfo.buttonLabel_size);
    setChannel = new Button(x + padding, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "CHANGE CHAN.", fontInfo.buttonLabel_size);
    autoscan = new Button(x + 2*padding + (w-padding*3)/2, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "AUTOSCAN", fontInfo.buttonLabel_size);
    ovrChannel = new Button(x + padding, y + padding*4 + 18 + 24*2, w-(padding*2), 24, "OVERRIDE DONGLE", fontInfo.buttonLabel_size);

    //Set help text
    getChannel.setHelpText("Get the current channel of your Cyton and USB Dongle");
    setChannel.setHelpText("Change the channel of your Cyton and USB Dongle");
    ovrChannel.setHelpText("Change the channel of the USB Dongle only");
    autoscan.setHelpText("Scan through channels and connect to a nearby Cyton");
    systemStatus.setHelpText("Get the connection status of your Cyton system");
  }
  public void update() {}

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("RADIO CONFIGURATION", x + padding, y + padding);
    popStyle();
    getChannel.draw();
    setChannel.draw();
    ovrChannel.draw();
    systemStatus.draw();
    autoscan.draw();

    this.print_onscreen(last_message);
  }

  public void print_onscreen(String localstring){
    textAlign(LEFT);
    fill(bgColor);
    rect(x + padding, y + (padding*8) + 13 + (24*2), w-(padding*2), 135 - 21 - padding);
    fill(255);
    text(localstring, x + padding + 10, y + (padding*8) + 5 + (24*2) + 15, (w-padding*3 ), 135 - 24 - padding -15);
    this.last_message = localstring;
  }

  public void print_lastmessage(){
    fill(bgColor);
    rect(x + padding, y + (padding*8) + 13 + (24*2), w-(padding*2), 135 - 21 - padding);
    fill(255);
    text(this.last_message, 180, 340, 240, 60);
  }
};

class WifiConfigBox {
  int x, y, w, h, padding; //size and position
  String last_message = "";
  Serial board;
  boolean isShowing;

  WifiConfigBox(int _x, int _y, int _w, int _h, int _padding) {
    x = _x + _w;
    y = _y;
    w = _w;
    h = 255;
    padding = _padding;
    isShowing = false;

    getTypeOfAttachedBoard = new Button(x + padding, y + padding*2 + 18, (w-padding*3)/2, 24, "OPENBCI BOARD", fontInfo.buttonLabel_size);
    getIpAddress = new Button(x + 2*padding + (w-padding*3)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "IP ADDRESS", fontInfo.buttonLabel_size);
    // getIpAddress = new Button(x + w -padding*2)/2, y + padding*2 + 18, (w-padding*3)/2, 24, "IP ADDRESS", fontInfo.buttonLabel_size);
    getMacAddress = new Button(x + padding, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "MAC ADDRESS", fontInfo.buttonLabel_size);
    getFirmwareVersion = new Button(x + 2*padding + (w-padding*3)/2, y + padding*3 + 18 + 24, (w-padding*3)/2, 24, "FIRMWARE VERS.", fontInfo.buttonLabel_size);
    eraseCredentials = new Button(x + padding, y + padding*4 + 18 + 24*2, w-(padding*2), 24, "ERASE NETWORK CREDENTIALS", fontInfo.buttonLabel_size);

    //y + padding*4 + 18 + 24*2

    //Set help text
    getTypeOfAttachedBoard.setHelpText("Get the type of OpenBCI board attached to the WiFi Shield");
    getIpAddress.setHelpText("Get the IP Address of the WiFi shield");
    getMacAddress.setHelpText("Get the MAC Address of the WiFi shield");
    getFirmwareVersion.setHelpText("Get the firmware version of the WiFi Shield");
    eraseCredentials.setHelpText("Erase the store credentials on the WiFi Shield to join another wireless network. Always remove WiFi Shield from OpenBCI board prior to erase and WiFi Shield will become a hotspot again.");
  }
  public void update() {}

  public void draw() {
    pushStyle();
    fill(boxColor);
    stroke(boxStrokeColor);
    strokeWeight(1);
    rect(x, y, w, h);
    fill(bgColor);
    textFont(h3, 16);
    textAlign(LEFT, TOP);
    text("WIFI CONFIGURATION", x + padding, y + padding);
    popStyle();
    getTypeOfAttachedBoard.draw();
    getIpAddress.draw();
    getMacAddress.draw();
    getFirmwareVersion.draw();
    eraseCredentials.draw();

    this.print_onscreen(last_message);
  }

  public void updateMessage(String str) {
    last_message = str;
  }

  public void print_onscreen(String localstring){
    textAlign(LEFT);
    fill(bgColor);
    rect(x + padding, y + (padding*8) + 13 + (24*2), w-(padding*2), 135 - 21 - padding);
    fill(255);
    text(localstring, x + padding + 10, y + (padding*8) + 5 + (24*2) + 15, (w-padding*3 ), 135 - 24 - padding -15);
    // this.last_message = localstring;


    // textAlign(LEFT);
    // fill(0);
    // rect(x + padding, y + (padding*8) + 18 + (24*2), (w-padding*3 + 5), 135 - 24 - padding);
    // fill(255);
    // text(localstring, x + padding + 10, y + (padding*8) + 18 + (24*2) + 15, (w-padding*3 ), 135 - 24 - padding -15);
  }

  public void print_lastmessage(){

    fill(bgColor);
    rect(x + padding, y + (padding*8) + 13 + (24*2), w-(padding*2), 135 - 21 - padding);
    fill(255);

    // fill(0);
    // rect(x + padding, y + (padding*7) + 18 + (24*5), (w-padding*3 + 5), 135);
    // fill(255);
    text(this.last_message, 180, 340, 240, 60);
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
    // autoconnect.draw();
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
    // autoconnect.draw();
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
    println("OpenBCI_GUI: closing log file");
    closeLogFile();
  }
  //open the new file
  fileoutput_bdf = new OutputFile_BDF(getSampleRateSafe(), nchan, _fileName);

  output_fname = fileoutput_bdf.fname;
  println("cyton: openNewLogFile: opened BDF output file: " + output_fname);
  output("cyton: openNewLogFile: opened BDF output file: " + output_fname);
}

/**
 * @description Opens (and closes if already open) and ODF file. ODF is the
 *  openbci data format.
 * @param `_fileName` {String} - The meat of the file name
 */
public void openNewLogFileODF(String _fileName) {
  if (fileoutput_odf != null) {
    println("OpenBCI_GUI: closing log file");
    closeLogFile();
  }
  //open the new file
  fileoutput_odf = new OutputFile_rawtxt(getSampleRateSafe(), _fileName);

  output_fname = fileoutput_odf.fname;
  println("cyton: openNewLogFile: opened ODF output file: " + output_fname);
  output("cyton: openNewLogFile: opened ODF output file: " + output_fname);
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
  logFileName = "SavedData/SDconverted-"+getDateString()+".csv";
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
  PrintWriter output_nn;
  String fname;
  String fname_nn;
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
    fname_nn = fname;
    //add the extension
    fname = fname + ".csv";
    fname_nn += "-NN.csv";

    //open the file
    output = createWriter(fname);
    output_nn = createWriter(fname_nn);
    //add the header
    writeHeader(fs_Hz);

    //init the counter
    rowsWritten = 0;
  }

  //variation on constructor to have custom name
  OutputFile_rawtxt(float fs_Hz, String _fileName) {
    fname = "SavedData"+System.getProperty("file.separator")+"OpenBCI-RAW-";
    fname += _fileName;
    fname_nn = fname;
    fname += ".csv";
    fname_nn += "-NN.csv";
    output = createWriter(fname);        //open the file
    output_nn = createWriter(fname_nn);
    writeHeader(fs_Hz);    //add the header
    rowsWritten = 0;    //init the counter
  }

  public void writeHeader(float fs_Hz) {
    output.println("%OpenBCI Raw EEG Data");
    output.println("%");
    output.println("%Sample Rate = " + fs_Hz + " Hz");
    output.println("%First Column = SampleIndex");
    output.println("%5 Columns before Timestamp are Frequency Power factors DELTA THETA ALPHA BETA GAMMA");
    output.println("%Last Column = Timestamp ");
    output.println("%Other Columns = EEG data in microvolts followed by Accel Data (in G) interleaved with Aux Data");
    output.flush();
    
    // add cleaned nn prefiltered file
    
    
    output_nn.flush();
    
  }

  public void writeRawData_dataPacket(DataPacket_ADS1299 data, float scale_to_uV, float scale_for_aux, int stopByte) {
    //get current date time with Date()
    Date date = new Date();
    

    if (output != null) {
      //output.print(Integer.toString(data.sampleIndex));  //removed for simple output
      writeValues(data.values,scale_to_uV);
      if (eegDataSource == DATASOURCE_GANGLION) {
        writeAccValues(data.auxValues, scale_for_aux);
      } else {
        if (stopByte == 0xC1) {
          writeAuxValues(data);
        } else {
          writeAccValues(data.auxValues, scale_for_aux);
        }
      }
     writeFrequencies();
      output.print( ", " + dateFormat.format(date));
      output.println(); rowsWritten++;
      
      // nn file putput
      output_nn.print( ", " + dateFormat.format(date));
      output_nn.println(); rowsWritten++;
      //output.flush();
    }
  }
  
  private void writeFrequencies(){
    output.print("," + validatePower(dataProcessing.headWidePower[DELTA]));
    output.print("," + validatePower(dataProcessing.headWidePower[THETA]));
    output.print("," + validatePower(dataProcessing.headWidePower[ALPHA]));
    output.print("," + validatePower(dataProcessing.headWidePower[BETA]));
    output.print("," + validatePower(dataProcessing.headWidePower[GAMMA]));
    
    // nn file output
    
    output_nn.print("," + validatePower(dataProcessing.headWidePower[DELTA]));
    output_nn.print("," + validatePower(dataProcessing.headWidePower[THETA]));
    output_nn.print("," + validatePower(dataProcessing.headWidePower[ALPHA]));
    output_nn.print("," + validatePower(dataProcessing.headWidePower[BETA]));
    output_nn.print("," + validatePower(dataProcessing.headWidePower[GAMMA]));
  }
  private float validatePower(float checkval){
    float cleaned = 0;
    if ((checkval > 100) || (checkval < 0)){
      cleaned = 0;
    } else
    {
       cleaned = checkval / 100;
    }
    return cleaned;
  }

  private void writeValues(int[] values, float scale_fac) {
    int nVal = values.length;
    for (int Ival = 0; Ival < nVal; Ival++) {
      
      //output.print(String.format(Locale.US, "%.2f", scale_fac * float(values[Ival])));  //original formula
      output.print(String.format(Locale.US, "%f", abs(scale_fac * PApplet.parseFloat(values[Ival]))   ));
      
      // nn file output
      
      output_nn.print(String.format(Locale.US, "%f", abs(scale_fac /10000  * PApplet.parseFloat(values[Ival])) /500   ));
      if((Ival +1 < nVal)) {
        output.print(", ");
        output_nn.print(", ");
      }
      
    }
  }

  private void writeAccValues(int[] values, float scale_fac) {
    int nVal = values.length;
    for (int Ival = 0; Ival < nVal; Ival++) {
      output.print(", ");
      output.print(String.format(Locale.US, "%.3f", scale_fac * PApplet.parseFloat(values[Ival])));
      
      output_nn.print(", ");
      output_nn.print(String.format(Locale.US, "%.3f", scale_fac * PApplet.parseFloat(values[Ival])));
    }
  }

  private void writeAuxValues(DataPacket_ADS1299 data) {
    if (eegDataSource == DATASOURCE_CYTON) {
      // println("board mode: " + cyton.getBoardMode());
      if (cyton.getBoardMode() == BOARD_MODE_DIGITAL) {
        if (marker_indicater > 0){
         data.auxValues[1] = 257; 
        }
        if (cyton.isWifi()) {
          output.print(", " + ((data.auxValues[0] & 0xFF00) >> 8));
          output.print(", " + (data.auxValues[0] & 0xFF));
          output.print(", " + data.auxValues[1]);
          
          output_nn.print(", " + ((data.auxValues[0] & 0xFF00) >> 8));
          output_nn.print(", " + (data.auxValues[0] & 0xFF));
          output_nn.print(", " + data.auxValues[1]);
        } else {
          output.print(", " + ((data.auxValues[0] & 0xFF00) >> 8));
          output.print(", " + (data.auxValues[0] & 0xFF));
          output.print(", " + ((data.auxValues[1] & 0xFF00) >> 8));
          output.print(", " + (data.auxValues[1] & 0xFF));
          output.print(", " + data.auxValues[2]);
          
          output_nn.print(", " + ((data.auxValues[0] & 0xFF00) >> 8));
          output_nn.print(", " + (data.auxValues[0] & 0xFF));
          output_nn.print(", " + ((data.auxValues[1] & 0xFF00) >> 8));
          output_nn.print(", " + (data.auxValues[1] & 0xFF));
          output_nn.print(", " + data.auxValues[2]);
          
        }
      } else if (cyton.getBoardMode() == BOARD_MODE_ANALOG) {
        if (cyton.isWifi()) {
          output.print(", " + data.auxValues[0]);
          output.print(", " + data.auxValues[1]);
          
          output_nn.print(", " + data.auxValues[0]);
          output_nn.print(", " + data.auxValues[1]);
        } else {
          output.print(", " + data.auxValues[0]);
          output.print(", " + data.auxValues[1]);
          output.print(", " + data.auxValues[2]);
          
          output_nn.print(", " + data.auxValues[0]);
          output_nn.print(", " + data.auxValues[1]);
          output_nn.print(", " + data.auxValues[2]);
         
        }
      } else if (cyton.getBoardMode() == BOARD_MODE_MARKER) {
        output.print(", " + data.auxValues[0]);
        
        output_nn.print(", " + data.auxValues[0]);
        if ( data.auxValues[0] > 0) {
          hub.validLastMarker = data.auxValues[0];
        }
          
      } else {
        for (int Ival = 0; Ival < 3; Ival++) {
          output.print(", " + data.auxValues[Ival]);
          
          output_nn.print(", " + data.auxValues[Ival]);
        }
      }
    } else {
      for (int i = 0; i < 3; i++) {
        output.print(", " + (data.auxValues[i] & 0xFF));
        output.print(", " + ((data.auxValues[i] & 0xFF00) >> 8));
        
        output_nn.print(", " + (data.auxValues[i] & 0xFF));
        output_nn.print(", " + ((data.auxValues[i] & 0xFF00) >> 8));
      }
    }
    

  }

  public void closeFile() {
    output.flush();
    output.close();
    
    output_nn.flush();
    output_nn.close();
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
    if (eegDataSource == DATASOURCE_CYTON) {
      writeAuxDataValues(data.rawAuxValues);
    }
    samplesInDataRecord++;
    // writeValues(data.auxValues,scale_for_aux);
    if (samplesInDataRecord >= fs_Hz) {
      arrayCopy(chanValBuf,chanValBuf_buffer);
      if (eegDataSource == DATASOURCE_CYTON) {
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
      if (eegDataSource == DATASOURCE_CYTON) {
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
    String output = "SavedData"+System.getProperty("file.separator")+"OpenBCI-BDF-";
    output += s;
    output += ".bdf";
    return output;
  }

  /**
   * @description Get's the number of signal channels to write out. Have to
   *  keep in mind that the annotations channel counts.
   * @returns {int} - The number of signals in the header.
   */
  private int getNbSignals() {
    if (eegDataSource == DATASOURCE_CYTON) {
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
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(labelsAux, BDF_HEADER_NS_SIZE_LABEL, o);
      writeStringArrayWithPaddingTimes(labelsAnnotations, BDF_HEADER_NS_SIZE_LABEL, o);

      writeStringArrayWithPaddingTimes(transducerEEG, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(transducerAux, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);
      writeStringArrayWithPaddingTimes(transducerAnnotations, BDF_HEADER_NS_SIZE_TRANSDUCER_TYPE, o);

      writeStringArrayWithPaddingTimes(physicalDimensionEEG, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(physicalDimensionAux, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);
      writeStringArrayWithPaddingTimes(physicalDimensionAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_DIMENSION, o);

      writeStringArrayWithPaddingTimes(physicalMinimumEEG, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(physicalMinimumAux, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);
      writeStringArrayWithPaddingTimes(physicalMinimumAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_MINIMUM, o);

      writeStringArrayWithPaddingTimes(physicalMaximumEEG, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(physicalMaximumAux, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);
      writeStringArrayWithPaddingTimes(physicalMaximumAnnotations, BDF_HEADER_NS_SIZE_PHYSICAL_MAXIMUM, o);

      writeStringArrayWithPaddingTimes(digitalMinimumEEG, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(digitalMinimumAux, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);
      writeStringArrayWithPaddingTimes(digitalMinimumAnnotations, BDF_HEADER_NS_SIZE_DIGITAL_MINIMUM, o);

      writeStringArrayWithPaddingTimes(digitalMaximumEEG, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(digitalMaximumAux, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);
      writeStringArrayWithPaddingTimes(digitalMaximumAnnotations, BDF_HEADER_NS_SIZE_DIGITAL_MAXIMUM, o);

      writeStringArrayWithPaddingTimes(prefilteringEEG, BDF_HEADER_NS_SIZE_PREFILTERING, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(prefilteringAux, BDF_HEADER_NS_SIZE_PREFILTERING, o);
      writeStringArrayWithPaddingTimes(prefilteringAnnotations, BDF_HEADER_NS_SIZE_PREFILTERING, o);

      writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordEEG, BDF_HEADER_NS_SIZE_NR, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordAux, BDF_HEADER_NS_SIZE_NR, o);
      writeStringArrayWithPaddingTimes(nbSamplesPerDataRecordAnnotations, BDF_HEADER_NS_SIZE_NR, o);

      writeStringArrayWithPaddingTimes(reservedEEG, BDF_HEADER_NS_SIZE_RESERVED, o);
      if (eegDataSource == DATASOURCE_CYTON) writeStringArrayWithPaddingTimes(reservedAux, BDF_HEADER_NS_SIZE_RESERVED, o);
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
  private int sampleRate;
  public int getSampleRate() { return sampleRate; }
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
          if (line.length() > 18) {
            if (line.charAt(1) == 'S') {
              // println(line.substring(15, 18));
              sampleRate = Integer.parseInt(line.substring(15, 18));
              if (sampleRate == 100 || sampleRate == 160) {
                sampleRate = Integer.parseInt(line.substring(15, 19));
              }
              println("Sample rate set to " + sampleRate);
              // String[] m = match(line, "\\d+");
              // if (m != null) {
                // println("Found '" + m[1] + "' inside the line");
              // }
            }
          }
          println(line);
          // if (line.charAt(1) == 'S') {
          //   println("sampel rarteakjdsf;ldj");
          // }
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
//    Updated: Joel Murphy - 6/26/17
//
//////////////////////////////////

//variables for SD file conversion
BufferedReader dataReader;
String dataLine;
PrintWriter dataWriter;
String convertedLine;
String thisLine;
String h;
float[] floatData = new float[20];
float[] intData = new float[20];
String logFileName;
String[] hexNums;
long thisTime;
long thatTime;
boolean printNextLine = false;

public void convertSDFile() {
  // println("");
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
    output("SD file converted to " + logFileName);
    dataWriter.flush();
    dataWriter.close();
  }
    else
  {
    hexNums = splitTokens(dataLine, ",");

    if (hexNums[0].charAt(0) == '%') {
      //          println(dataLine);
      dataWriter.println(dataLine);
      println(dataLine);
      printNextLine = true;
    } else {
      if (hexNums.length < 13){
        convert8channelLine();
      } else {
        convert16channelLine();
      }
      if(printNextLine){
        printNextLine = false;
      }
    }
  }
}

public void convert16channelLine() {
  if(printNextLine){
    for(int i=0; i<hexNums.length; i++){
      h = hexNums[i];
      if (h.length()%2 == 0) {  // make sure this is a real number
        intData[i] = unhex(h);
      } else {
        intData[i] = 0;
      }
      dataWriter.print(intData[i]);
      print(intData[i]);
      if(hexNums.length > 1){
        dataWriter.print(", ");
        print(", ");
      }
    }
    dataWriter.println();
    println();
    return;
  }
  for (int i=0; i<hexNums.length; i++) {
    h = hexNums[i];
    if (i > 0) {
      if (h.charAt(0) > '7') {  // if the number is negative
        h = "FF" + hexNums[i];   // keep it negative
      } else {                  // if the number is positive
        h = "00" + hexNums[i];   // keep it positive
      }
      if (i > 16) { // accelerometer data needs another byte
        if (h.charAt(0) == 'F') {
          h = "FF" + h;
        } else {
          h = "00" + h;
        }
      }
    }
    // println(h); // use for debugging
    if (h.length()%2 == 0) {  // make sure this is a real number
      floatData[i] = unhex(h);
    } else {
      floatData[i] = 0;
    }

    if (i>=1 && i<=16) {
      floatData[i] *= cyton.get_scale_fac_uVolts_per_count();
    }else if(i != 0){
      floatData[i] *= cyton.get_scale_fac_accel_G_per_count();
    }

    if(i == 0){
      dataWriter.print(PApplet.parseInt(floatData[i]));  // print the sample counter
    }else{
      dataWriter.print(floatData[i]);  // print the current channel value
    }
    if (i < hexNums.length-1) {  // print the current channel value
      dataWriter.print(",");  // print "," separator
    }
  }
  dataWriter.println();
}

public void convert8channelLine() {
  if(printNextLine){
    for(int i=0; i<hexNums.length; i++){
      h = hexNums[i];
      if (h.length()%2 == 0) {  // make sure this is a real number
        intData[i] = unhex(h);
      } else {
        intData[i] = 0;
      }
      print(intData[i]);
      dataWriter.print(intData[i]);
      if(hexNums.length > 1){
        dataWriter.print(", ");
        print(", ");
      }
    }
    dataWriter.println();
    println();
    return;
  }
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
      floatData[i] = unhex(h);
    } else {
      floatData[i] = 0;
    }

    if (i>=1 && i<=8) {
      floatData[i] *= cyton.get_scale_fac_uVolts_per_count();
    }else if(i != 0){
      floatData[i] *= cyton.get_scale_fac_accel_G_per_count();
    }

    if(i == 0){
      dataWriter.print(PApplet.parseInt(floatData[i]));  // print the sample counter
    }else{
      dataWriter.print(floatData[i]);  // print the current channel value
    }
    if (i < hexNums.length-1) {
      dataWriter.print(",");  // print "," separator
    }
  }
  dataWriter.println();
}











//     BEWARE: Old Stuff Below
//
//     //        println(dataLine);
//     String[] hexNums = splitTokens(dataLine, ",");
//
//     if (hexNums[0].charAt(0) == '%') {
//       //          println(dataLine);
//       dataWriter.println(dataLine);
//       println(dataLine);
//       printNextLine = true;
//     } else {
//       for (int i=0; i<hexNums.length; i++) {
//         h = hexNums[i];
//         if (i > 0) {
//           if (h.charAt(0) > '7') {  // if the number is negative
//             h = "FF" + hexNums[i];   // keep it negative
//           } else {                  // if the number is positive
//             h = "00" + hexNums[i];   // keep it positive
//           }
//           if (i > 8) { // accelerometer data needs another byte
//             if (h.charAt(0) == 'F') {
//               h = "FF" + h;
//             } else {
//               h = "00" + h;
//             }
//           }
//         }
//         // println(h); // use for debugging
//         if (h.length()%2 == 0) {  // make sure this is a real number
//           intData[i] = unhex(h);
//         } else {
//           intData[i] = 0;
//         }
//
//         //if not first column(sample #) or columns 9-11 (accelerometer), convert to uV
//         if (i>=1 && i<=8) {
//           intData[i] *= openBCI.get_scale_fac_uVolts_per_count();
//         }
//
//         //print the current channel value
//         dataWriter.print(intData[i]);
//         if (i < hexNums.length-1) {
//           //print "," separator
//           dataWriter.print(",");
//         }
//       }
//       //println();
//       dataWriter.println();
//     }
//   }
// }

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

// indexs
final int DELTA = 0; // 1-4 Hz
final int THETA = 1; // 4-8 Hz
final int ALPHA = 2; // 8-13 Hz
final int BETA = 3; // 13-30 Hz
final int GAMMA = 4; // 30-55 Hz


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
      currentTableRowIndex=getPlaybackDataFromTable(playbackData_table, currentTableRowIndex, cyton.get_scale_fac_uVolts_per_count(), cyton.get_scale_fac_accel_G_per_count(), dataPacketBuff[lastReadDataPacketInd]);

      for (int Ichan=0; Ichan < nchan; Ichan++) {
        //scale the data into engineering units..."microvolts"
        localLittleBuff[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan]* cyton.get_scale_fac_uVolts_per_count();
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

  if (eegDataSource == DATASOURCE_CYTON) {
    //get data from serial port as it streams in
    //next, gather any new data into the "little buffer"
    while ( (curDataPacketInd != lastReadDataPacketInd) && (pointCounter < nPointsPerUpdate)) {
      lastReadDataPacketInd = (lastReadDataPacketInd+1) % dataPacketBuff.length;  //increment to read the next packet
      for (int Ichan=0; Ichan < nchan; Ichan++) {   //loop over each cahnnel
        //scale the data into engineering units ("microvolts") and save to the "little buffer"
        yLittleBuff_uV[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan] * cyton.get_scale_fac_uVolts_per_count();
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
      int increment_millis = PApplet.parseInt(round(PApplet.parseFloat(nPointsPerUpdate)*1000.f/getSampleRateSafe())/playback_speed_fac);
      if (nextPlayback_millis < 0) nextPlayback_millis = current_millis;
      nextPlayback_millis += increment_millis;

      // generate or read the data
      lastReadDataPacketInd = 0;
      for (int i = 0; i < nPointsPerUpdate; i++) {
        // println();
        dataPacketBuff[lastReadDataPacketInd].sampleIndex++;
        switch (eegDataSource) {
        case DATASOURCE_SYNTHETIC: //use synthetic data (for GUI debugging)
          synthesizeData(nchan, getSampleRateSafe(), cyton.get_scale_fac_uVolts_per_count(), dataPacketBuff[lastReadDataPacketInd]);
          break;
        case DATASOURCE_PLAYBACKFILE:
          currentTableRowIndex=getPlaybackDataFromTable(playbackData_table, currentTableRowIndex, cyton.get_scale_fac_uVolts_per_count(), cyton.get_scale_fac_accel_G_per_count(), dataPacketBuff[lastReadDataPacketInd]);
          break;
        default:
          //no action
        }
        //gather the data into the "little buffer"
        for (int Ichan=0; Ichan < nchan; Ichan++) {
          //scale the data into engineering units..."microvolts"
          yLittleBuff_uV[Ichan][pointCounter] = dataPacketBuff[lastReadDataPacketInd].values[Ichan]* cyton.get_scale_fac_uVolts_per_count();
        }

        pointCounter++;
      } //close the loop over data points
      //if (eegDataSource==DATASOURCE_PLAYBACKFILE) println("OpenBCI_GUI: getDataIfAvailable: currentTableRowIndex = " + currentTableRowIndex);
      //println("OpenBCI_GUI: getDataIfAvailable: pointCounter = " + pointCounter);
    } // close "has enough time passed"
    else{
    }
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
  dataProcessing.newDataToSend = true;

  //look to see if the latest data is railed so that we can notify the user on the GUI
  for (int Ichan=0; Ichan < nchan; Ichan++) is_railed[Ichan].update(dataPacketBuff[lastReadDataPacketInd].values[Ichan]);

  //compute the electrode impedance. Do it in a very simple way [rms to amplitude, then uVolt to Volt, then Volt/Amp to Ohm]
  for (int Ichan=0; Ichan < nchan; Ichan++) {
    // Calculate the impedance
    float impedance = (sqrt(2.0f)*dataProcessing.data_std_uV[Ichan]*1.0e-6f) / cyton.get_leadOffDrive_amps();
    // Subtract the 2.2kOhm resistor
    impedance -= cyton.get_series_resistor();
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


public void initializeFFTObjects(FFT[] fftBuff, float[][] dataBuffY_uV, int Nfft, float fs_Hz) {

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
    println("OpenBCI_GUI: getPlaybackDataFromTable: hit the end of the playback data file.  starting over...");
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
        row.getString(nchan+3);

        // nchan = 16; AJK 5/31/17 see issue #151
      }
      catch (ArrayIndexOutOfBoundsException e){
        println(e);
        println("8 Channel");
      }
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
  boolean newDataToSend;
  private String[] binNames;
  final int[] processing_band_low_Hz = {
    1, 4, 8, 13, 30
  }; //lower bound for each frequency band of interest (2D classifier only)
  final int[] processing_band_high_Hz = {
    4, 8, 13, 30, 55
  };  //upper bound for each frequency band of interest
  float avgPowerInBins[][];
  float headWidePower[];
  int numBins;

  DataProcessing(int NCHAN, float sample_rate_Hz) {
    nchan = NCHAN;
    fs_Hz = sample_rate_Hz;
    data_std_uV = new float[nchan];
    polarity = new float[nchan];
    newDataToSend = false;
    avgPowerInBins = new float[nchan][processing_band_low_Hz.length];
    headWidePower = new float[processing_band_low_Hz.length];

    defineFilters();  //define the filters anyway just so that the code doesn't bomb
  }

  //define filters depending on the sampling rate
  private void defineFilters() {
    int n_filt;
    double[] b, a, b2, a2;
    String filt_txt, filt_txt2;
    String short_txt, short_txt2;

    //------------ loop over all of the pre-defined filter types -----------
    //------------ notch filters ------------
    n_filt = filtCoeff_notch.length;
    for (int Ifilt=0; Ifilt < n_filt; Ifilt++) {
      switch (Ifilt) {
        case 0:
          //60 Hz notch filter, 2nd Order Butterworth: [b, a] = butter(2,[59.0 61.0]/(fs_Hz / 2.0), 'stop') %matlab command
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b2 = new double[] { 0.931378858122982f, 3.70081291785747f, 5.53903191270520f, 3.70081291785747f, 0.931378858122982f };
              a2 = new double[] { 1, 3.83246204081167f, 5.53431749515949f, 3.56916379490328f, 0.867472133791669f };
              break;
            case 200:
              b2 = new double[] { 0.956543225556877f, 1.18293615779028f, 2.27881429174348f, 1.18293615779028f, 0.956543225556877f };
              a2 = new double[] { 1, 1.20922304075909f, 2.27692490805580f, 1.15664927482146f, 0.914975834801436f };
              break;
            case 250:
              b2 = new double[] { 0.965080986344733f, -0.242468320175764f, 1.94539149412878f, -0.242468320175764f, 0.965080986344733f };
              a2 = new double[] { 1, -0.246778261129785f, 1.94417178469135f, -0.238158379221743f, 0.931381682126902f };
              break;
            case 500:
              b2 = new double[] { 0.982385438526095f, -2.86473884662109f, 4.05324051877773f, -2.86473884662109f, 0.982385438526095f};
              a2 = new double[] { 1, -2.89019558531207f, 4.05293022193077f, -2.83928210793009f, 0.965081173899134f };
              break;
            case 1000:
              b2 = new double[] { 0.991153595101611f, -3.68627799048791f, 5.40978944177152f, -3.68627799048791f, 0.991153595101611f };
              a2 = new double[] { 1, -3.70265590760266f, 5.40971118136100f, -3.66990007337352f, 0.982385450614122f };
              break;
            case 1600:
              b2 = new double[] { 0.994461788958027f, -3.86796874670208f, 5.75004904085114f, -3.86796874670208f, 0.994461788958027f };
              a2 = new double[] { 1, -3.87870938463296f, 5.75001836883538f, -3.85722810877252f, 0.988954249933128f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b2 = new double[] { 1.0f };
              a2 = new double[] { 1.0f };
          }
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 60Hz", "60Hz");
          break;
        case 1:
          //50 Hz notch filter, 2nd Order Butterworth: [b, a] = butter(2,[49.0 51.0]/(fs_Hz / 2.0), 'stop')
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b2 = new double[] { 0.931378858122983f, 3.01781693143160f, 4.30731047590091f, 3.01781693143160f, 0.931378858122983f };
              a2 = new double[] { 1, 3.12516981877757f, 4.30259605835520f, 2.91046404408562f, 0.867472133791670f };
              break;
            case 200:
              b2 = new double[] { 0.956543225556877f, -2.34285519884863e-16f, 1.91308645111375f, -2.34285519884863e-16f, 0.956543225556877f };
              a2 = new double[] { 1, -1.41553435639707e-15f, 1.91119706742607f, -1.36696209906972e-15f, 0.914975834801435f };
              break;
            case 250:
              b2 = new double[] { 0.965080986344734f, -1.19328255433335f, 2.29902305135123f, -1.19328255433335f, 0.965080986344734f };
              a2 = new double[] { 1, -1.21449347931898f, 2.29780334191380f, -1.17207162934771f, 0.931381682126901f };
              break;
            case 500:
              b2 = new double[] { 0.982385438526090f, -3.17931708468811f, 4.53709552901242f, -3.17931708468811f, 0.982385438526090f };
              a2 = new double[] { 1, -3.20756923909868f, 4.53678523216547f, -3.15106493027754f, 0.965081173899133f };
              break;
            case 1000:
              b2 = new double[] { 0.991153595101607f, -3.77064677042206f, 5.56847615976560f, -3.77064677042206f, 0.991153595101607f };
              a2 = new double[] { 1, -3.78739953308251f, 5.56839789935513f, -3.75389400776205f, 0.982385450614127f };
              break;
            case 1600:
              b2 = new double[] { 0.994461788958316f, -3.90144402068168f, 5.81543195046478f, -3.90144402068168f, 0.994461788958316f };
              a2 = new double[] { 1, -3.91227761329151f, 5.81540127844733f, -3.89061042807090f, 0.988954249933127f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b2 = new double[] { 1.0f };
              a2 = new double[] { 1.0f };
          }
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "Notch 50Hz", "50Hz");
          break;
        case 2:
          //no notch filter
          b2 = new double[] { 1.0f };
          a2 = new double[] { 1.0f };
          filtCoeff_notch[Ifilt] =  new FilterConstants(b2, a2, "No Notch", "None");
          break;
        }
      }// end loop over notch filters

      //------------ bandpass filters ------------
      n_filt = filtCoeff_bp.length;
      for (int Ifilt=0; Ifilt<n_filt; Ifilt++) {
        //define bandpass filter
        switch (Ifilt) {
        case 0:
          //1-50 Hz band pass filter, 2nd Order Butterworth: [b, a] = butter(2,[1.0 50.0]/(fs_Hz / 2.0))
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b = new double[] { 0.615877232553135f, 0, -1.23175446510627f, 0, 0.615877232553135f };
              a = new double[] { 1, -0.789307541613509f, -0.853263915766877f, 0.263710995896442f, 0.385190413112446f };
              break;
            case 200:
              b = new double[] { 0.283751216219319f, 0, -0.567502432438638f, 0, 0.283751216219319f };
              a = new double[] { 1, -1.97380379923172f, 1.17181238127012f, -0.368664525962831f, 0.171812381270120f };
              break;
            case 250:
              b = new double[] { 0.200138725658073f, 0, -0.400277451316145f, 0, 0.200138725658073f };
              a = new double[] { 1, -2.35593463113158f, 1.94125708865521f, -0.784706375533419f, 0.199907605296834f };
              break;
            case 500:
              b = new double[] { 0.0652016551604422f, 0, -0.130403310320884f, 0, 0.0652016551604422f };
              a = new double[] { 1, -3.14636562553919f, 3.71754597063790f, -1.99118301927812f, 0.420045500522989f };
              break;
            case 1000:
              b = new double[] { 0.0193615659240911f, 0, -0.0387231318481823f, 0, 0.0193615659240911f };
              a = new double[] { 1, -3.56607203834158f, 4.77991824545949f, -2.86091191298975f, 0.647068888346475f };
              break;
            case 1600:
              b = new double[] { 0.00812885687466408f, 0, -0.0162577137493282f, 0, 0.00812885687466408f };
              a = new double[] { 1, -3.72780746887970f, 5.21756471024747f, -3.25152171857009f, 0.761764999239264f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b = new double[] { 1.0f };
              a = new double[] { 1.0f };
          }
          filt_txt = "Bandpass 1-50Hz";
          short_txt = "1-50 Hz";
          break;
        case 1:
          //7-13 Hz band pass filter, 2nd Order Butterworth: [b, a] = butter(2,[7.0 13.0]/(fs_Hz / 2.0))
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b = new double[] { 0.0186503962278349f, 0, -0.0373007924556699f, 0, 0.0186503962278349f };
              a = new double[] { 1, -3.17162467236842f, 4.11670870329067f, -2.55619949640702f, 0.652837763407545f };
              break;
            case 200:
              b = new double[] { 0.00782020803349772f, 0, -0.0156404160669954f, 0, 0.00782020803349772f };
              a = new double[] { 1, -3.56776916484310f, 4.92946172209398f, -3.12070317627516f, 0.766006600943265f };
              break;
            case 250:
              b = new double[] { 0.00512926836610803f, 0, -0.0102585367322161f, 0, 0.00512926836610803f };
              a = new double[] { 1, -3.67889546976404f, 5.17970041352212f, -3.30580189001670f, 0.807949591420914f };
              break;
            case 500:
              b = new double[] { 0.00134871194834618f, 0, -0.00269742389669237f, 0, 0.00134871194834618f };
              a = new double[] { 1, -3.86550956895320f, 5.63152598761351f, -3.66467991638185f, 0.898858994155253f };
              break;
            case 1000:
              b = new double[] { 0.000346041337684191f, 0, -0.000692082675368382f, 0, 0.000346041337684191f };
              a = new double[] { 1, -3.93960949694447f, 5.82749974685320f, -3.83595939375067f, 0.948081706106736f };
              break;
            case 1600:
              b = new double[] { 0.000136510722194708f, 0, -0.000273021444389417f, 0, 0.000136510722194708f };
              a = new double[] { 1, -3.96389829181139f, 5.89507193593518f, -3.89839913574117f, 0.967227428151860f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b = new double[] { 1.0f };
              a = new double[] { 1.0f };
          }
          filt_txt = "Bandpass 7-13Hz";
          short_txt = "7-13 Hz";
          break;
        case 2:
          //15-50 Hz band pass filter, 2nd Order Butterworth: [b, a] = butter(2,[15.0 50.0]/(fs_Hz / 2.0))
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b = new double[] { 0.350346377855414f, 0, -0.700692755710828f, 0, 0.350346377855414f };
              a = new double[] { 1, 0.175228265043619f, -0.211846955102387f, 0.0137230352398757f, 0.180232073898346f };
              break;
            case 200:
              b = new double[] { 0.167483800127017f, 0, -0.334967600254034f, 0, 0.167483800127017f };
              a = new double[] { 1, -1.56695061045088f, 1.22696619781982f, -0.619519163981229f, 0.226966197819818f };
              break;
            case 250:
              b = new double[] { 0.117351036724609f, 0, -0.234702073449219f, 0, 0.117351036724609f };
              a = new double[] { 1, -2.13743018017206f, 2.03857800810852f, -1.07014439920093f, 0.294636527587914f };
              break;
            case 500:
              b = new double[] { 0.0365748358439273f, 0, -0.0731496716878546f, 0, 0.0365748358439273f };
              a = new double[] { 1, -3.18880661866679f, 3.98037203788323f, -2.31835989524663f, 0.537194624801103f };
              break;
            case 1000:
              b = new double[] { 0.0104324133710872f, 0, -0.0208648267421744f, 0, 0.0104324133710872f };
              a = new double[] { 1, -3.63626742713985f, 5.01393973667604f, -3.10964559897057f, 0.732726030371817f };
              break;
            case 1600:
              b = new double[] { 0.00429884732196394f, 0, -0.00859769464392787f, 0, 0.00429884732196394f };
              a = new double[] { 1, -3.78412985599134f, 5.39377521548486f, -3.43287342581222f, 0.823349595537562f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b = new double[] { 1.0f };
              a = new double[] { 1.0f };
          }
          filt_txt = "Bandpass 15-50Hz";
          short_txt = "15-50 Hz";
          break;
        case 3:
          //5-50 Hz band pass filter, 2nd Order Butterworth: [b, a] = butter(2,[5.0 50.0]/(fs_Hz / 2.0))
          switch(PApplet.parseInt(fs_Hz)) {
            case 125:
              b = new double[] { 0.529967227069348f, 0, -1.05993445413870f, 0, 0.529967227069348f };
              a = new double[] { 1, -0.517003774490767f, -0.734318454224823f, 0.103843398397761f, 0.294636527587914f };
              break;
            case 200:
              b = new double[] { 0.248341078962541f, 0, -0.496682157925081f, 0, 0.248341078962541f };
              a = new double[] { 1, -1.86549482213123f, 1.17757811892770f, -0.460665534278457f, 0.177578118927698f };
              break;
            case 250:
              b = new double[] { 0.175087643672101f, 0, -0.350175287344202f, 0, 0.175087643672101f };
              a = new double[] { 1, -2.29905535603850f, 1.96749775998445f, -0.874805556449481f, 0.219653983913695f };
              break;
            case 500:
              b = new double[] { 0.0564484622607352f, 0, -0.112896924521470f, 0, 0.0564484622607352f };
              a = new double[] { 1, -3.15946330211917f, 3.79268442285094f, -2.08257331718360f, 0.450445430056042f };
              break;
            case 1000:
              b = new double[] { 0.0165819316692804f, 0, -0.0331638633385608f, 0, 0.0165819316692804f };
              a = new double[] { 1, -3.58623980811691f, 4.84628980428803f, -2.93042721682014f, 0.670457905953175f };
              break;
            case 1600:
              b = new double[] { 0.00692579317243661f, 0, -0.0138515863448732f, 0, 0.00692579317243661f };
              a = new double[] { 1, -3.74392328264678f, 5.26758817627966f, -3.30252568902969f, 0.778873972655117f };
              break;
            default:
              println("EEG_Processing: *** ERROR *** Filters can only work at 125Hz, 200Hz, 250 Hz, 1000Hz or 1600Hz");
              b = new double[] { 1.0f };
              a = new double[] { 1.0f };
          }
          filt_txt = "Bandpass 5-50Hz";
          short_txt = "5-50 Hz";
          break;
        default:
          //no filtering
          b = new double[] { 1.0f };
          a = new double[] { 1.0f };
          filt_txt = "No BP Filter";
          short_txt = "No Filter";
        }  //end switch block

        //create the bandpass filter
        filtCoeff_bp[Ifilt] =  new FilterConstants(b, a, filt_txt, short_txt);
    } //end loop over band pass filters
  }
  //end defineFilters method

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
    int Nfft = getNfftSafe();
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

      // FFT ref: https://www.mathworks.com/help/matlab/ref/fft.html
      // first calculate double-sided FFT amplitude spectrum
      for (int I=0; I <= Nfft/2; I++) {
        fftBuff[Ichan].setBand(I, (float)(fftBuff[Ichan].getBand(I) / Nfft));
      }
      // then convert into single-sided FFT spectrum: DC & Nyquist (i=0 & i=N/2) remain the same, others multiply by two.
      for (int I=1; I < Nfft/2; I++) {
        fftBuff[Ichan].setBand(I, (float)(fftBuff[Ichan].getBand(I) * 2));
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
        // fftBuff[Ichan].setBand(I, 1.0f);  // test
      } //end loop over FFT bins

      // calculate single-sided psd by single-sided FFT amplitude spectrum
      // PSD ref: https://www.mathworks.com/help/dsp/ug/estimate-the-power-spectral-density-in-matlab.html
      // when i = 1 ~ (N/2-1), psd = (N / fs) * mag(i)^2 / 4
      // when i = 0 or i = N/2, psd = (N / fs) * mag(i)^2

      for (int i = 0; i < processing_band_low_Hz.length; i++) {
        float sum = 0;
        // int binNum = 0;
        for (int Ibin = 0; Ibin <= Nfft/2; Ibin ++) { // loop over FFT bins
          float FFT_freq_Hz = fftBuff[Ichan].indexToFreq(Ibin);   // center frequency of this bin
          float psdx = 0;
          // if the frequency matches a band
          if (FFT_freq_Hz >= processing_band_low_Hz[i] && FFT_freq_Hz < processing_band_high_Hz[i]) {
            if (Ibin != 0 && Ibin != Nfft/2) {
              psdx = fftBuff[Ichan].getBand(Ibin) * fftBuff[Ichan].getBand(Ibin) * Nfft/getSampleRateSafe() / 4;
            }
            else {
              psdx = fftBuff[Ichan].getBand(Ibin) * fftBuff[Ichan].getBand(Ibin) * Nfft/getSampleRateSafe();
            }
            sum += psdx;
            // binNum ++;
          }
        }
        avgPowerInBins[Ichan][i] = sum;   // total power in a band
        // println(i, binNum, sum);
      }
    } //end the loop over channels.
    for (int i = 0; i < processing_band_low_Hz.length; i++) {
      float sum = 0;

      for (int j = 0; j < nchan; j++) {
        sum += avgPowerInBins[j][i];
      }
      headWidePower[i] = sum/nchan;   // averaging power over all channels
    }

    //delta in channel 2 ... avgPowerInBins[1][DELTA];
    //headwide beta ... headWidePower[BETA];

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

    // println("Brain Wide DELTA = " + headWidePower[DELTA]);
    // println("Brain Wide THETA = " + headWidePower[THETA]);
    // println("Brain Wide ALPHA = " + headWidePower[ALPHA]);
    // println("Brain Wide BETA  = " + headWidePower[BETA]);
    // println("Brain Wide GAMMA = " + headWidePower[GAMMA]);

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

final static int OUTPUT_LEVEL_DEFAULT = 0;
final static int OUTPUT_LEVEL_INFO = 1;
final static int OUTPUT_LEVEL_SUCCESS = 2;
final static int OUTPUT_LEVEL_WARN = 3;
final static int OUTPUT_LEVEL_ERROR = 4;

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

  String currentOutput = "Learn how to use this application and more at docs.openbci.com/OpenBCI%20Software/01-OpenBCI_GUI"; //current text shown in help widget, based on most recent command

  int padding = 5;
  int outputStart = 0;
  int outputDurationMs = 3000;
  boolean animatingMessage = false;
  int curOutputLevel = OUTPUT_LEVEL_DEFAULT;

  HelpWidget(float _xPos, float _yPos, float _width, float _height) {
    x = _xPos;
    y = _yPos;
    w = _width;
    h = _height;
  }

  public void update() {
    if (animatingMessage) {
      if (millis() > outputStart + outputDurationMs) {
        animatingMessage = false;
        curOutputLevel = OUTPUT_LEVEL_DEFAULT;
      }
    }
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
      stroke(getBackgroundColor());
      // fill(200);
      // fill(255);
      fill(getBackgroundColor());
      // fill(57,128,204);
      rect(x + padding, height-h + padding, width - padding*2, h - padding *2);

      textFont(p4);
      textSize(14);
      // fill(bgColor);
      fill(getTextColor());
      // fill(57,128,204);
      // fill(openbciBlue);
      textAlign(LEFT, TOP);
      text(currentOutput, padding*2, height - h + padding);
    }

    popStyle();
  }

  private int getTextColor() {
    switch (curOutputLevel) {
      case OUTPUT_LEVEL_INFO:
        return 0xff00529B;
      case OUTPUT_LEVEL_SUCCESS:
        return 0xff4F8A10;
      case OUTPUT_LEVEL_WARN:
        return 0xff9F6000;
      case OUTPUT_LEVEL_ERROR:
        return 0xffD8000C;
      case OUTPUT_LEVEL_DEFAULT:
      default:
        return color(0, 5, 11);
    }
  }

  private int getBackgroundColor() {
    switch (curOutputLevel) {
      case OUTPUT_LEVEL_INFO:
        return 0xffBDE5F8;
      case OUTPUT_LEVEL_SUCCESS:
        return 0xffDFF2BF;
      case OUTPUT_LEVEL_WARN:
        return 0xffFEEFB3;
      case OUTPUT_LEVEL_ERROR:
        return 0xffFFD2D2;
      case OUTPUT_LEVEL_DEFAULT:
      default:
        return color(255);
    }
  }

  public void output(String _output, int level) {
    if (OUTPUT_LEVEL_DEFAULT == level) {
      animatingMessage = false;
    } else {
      animatingMessage = true;
      outputStart = millis();
    }
    curOutputLevel = level;
    currentOutput = _output;
    // prevOutputs.add(_output);
  }
};

public void output(String _output) {
  output(_output, OUTPUT_LEVEL_DEFAULT);
}

public void output(String _output, int level) {
  helpWidget.output(_output, level);
}

public void outputError(String _output) {
  output(_output, OUTPUT_LEVEL_ERROR);
}

public void outputInfo(String _output) {
  output(_output, OUTPUT_LEVEL_INFO);
}

public void outputSuccess(String _output) {
  output(_output, OUTPUT_LEVEL_SUCCESS);
}

public void outputWarn(String _output) {
  output(_output, OUTPUT_LEVEL_WARN);
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
  println("OpenBCI_GUI: activating channel " + (Ichan+1));
  if (eegDataSource == DATASOURCE_CYTON) {
    if (cyton.isPortOpen()) {
      verbosePrint("**");
      cyton.changeChannelState(Ichan, true); //activate
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
  println("OpenBCI_GUI: deactivating channel " + (Ichan+1));
  if (eegDataSource == DATASOURCE_CYTON) {
    if (cyton.isPortOpen()) {
      verbosePrint("**");
      cyton.changeChannelState(Ichan, false); //de-activate
    }
  } else if (eegDataSource == DATASOURCE_GANGLION) {
    ganglion.changeChannelState(Ichan, false);
  }
  if (Ichan < nchan) {
    channelSettingValues[Ichan][0] = '1';
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

  // int numSettingsPerChannel = 6; //each channel has 6 different settings
  // char[][] channelSettingValues = new char [nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
  // char[][] impedanceCheckValues = new char [nchan][2];

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

    // AJ KELLER
    // if (cyton.get_isWritingChannel()) {
    //   cyton.writeChannelSettings(channelToWrite,channelSettingValues);
    // }

    // if (rewriteChannelWhenDoneWriting == true) {
    //   initChannelWrite(channelToWriteWhenDoneWriting);
    //   rewriteChannelWhenDoneWriting = false;
    // }

    // if (cyton.get_isWritingImp()) {
    //   cyton.writeImpedanceSettings(impChannelToWrite,impedanceCheckValues);
    // }

    if (rewriteImpedanceWhenDoneWriting == true && cyton.get_isWritingImp() == false) {
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
           fill(bgColor);
           textFont(p6, 10);
           textAlign(CENTER, TOP);
           text("PGA Gain", x + (w/10)*1, y-1);
           text("Input Type", x + (w/10)*3, y-1);
           text("  Bias ", x + (w/10)*5, y-1);
           text("SRB2", x + (w/10)*7, y-1);
           text("SRB1", x + (w/10)*9, y-1);

          //if mode is not from OpenBCI, draw a dark overlay to indicate that you cannot edit these settings
          if (eegDataSource != DATASOURCE_CYTON) {
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
    // println("loadDefaultChannelSettings");
    // verbosePrint("ChannelController: loading default channel settings to GUI's channel controller...");
    for (int i = 0; i < nchan; i++) {
      // verbosePrint("chan: " + i + " ");
      channelSettingValues[i][0] = '0';
      channelSettingValues[i][1] = '6';
      channelSettingValues[i][2] = '0';
      channelSettingValues[i][3] = '1';
      channelSettingValues[i][4] = '1';
      channelSettingValues[i][5] = '0';
      // for (int j = 0; j < numSettingsPerChannel; j++) { //channel setting values
      //   channelSettingValues[i][j] = char(cyton.get_defaultChannelSettings().toCharArray()[j]); //parse defaultChannelSettings string created in the Cyton class
      //   if (j == numSettingsPerChannel - 1) {
      //     println(char(cyton.get_defaultChannelSettings().toCharArray()[j]));
      //   } else {
      //     print(char(cyton.get_defaultChannelSettings().toCharArray()[j]) + ",");
      //   }
      // }
      for (int k = 0; k < 2; k++) { //impedance setting values
        impedanceCheckValues[i][k] = '0';
      }
    }
    // verbosePrint("made it!");
    update(); //update 1 time to refresh button values based on new loaded settings
  }

  // void updateChannelArrays(int _nchan) {
  //   channelSettingValues = new char [_nchan][numSettingsPerChannel]; // [channel#][Button#-value] ... this will incfluence text of button
  //   impedanceCheckValues = new char [_nchan][2];
  // }

  //activateChannel: Ichan is [0 nchan-1] (aka zero referenced)
  public void activateChannel(int Ichan) {
    println("OpenBCI_GUI: activating channel " + (Ichan+1));
    if (eegDataSource == DATASOURCE_CYTON) {
      if (cyton.isPortOpen()) {
        verbosePrint("**");
        cyton.changeChannelState(Ichan, true); //activate
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
    println("OpenBCI_GUI: deactivating channel " + (Ichan+1));
    if (eegDataSource == DATASOURCE_CYTON) {
      if (cyton.isPortOpen()) {
        verbosePrint("**");
        cyton.changeChannelState(Ichan, false); //de-activate
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
    cyton.deactivateChannel(_numChannel);  //assumes numChannel counts from zero (not one)...handles regular and daisy channels
  }

  public void powerUpChannel(int _numChannel) {
    verbosePrint("Powering up channel " + str(PApplet.parseInt(_numChannel) + PApplet.parseInt(1)));
    //replace SRB2 and BIAS settings with values from 2D history array
    channelSettingValues[_numChannel][3] = previousBIAS[_numChannel];
    channelSettingValues[_numChannel][4] = previousSRB2[_numChannel];

    channelSettingValues[_numChannel][0] = '0'; //update powerUp/powerDown value of 2D array
    verbosePrint("Command: " + command_activate_channel[_numChannel]);
    cyton.activateChannel(_numChannel);  //assumes numChannel counts from zero (not one)...handles regular and daisy channels//assumes numChannel counts from zero (not one)...handles regular and daisy channels
  }

  public void initChannelWrite(int _numChannel) {
    //after clicking any button, write the new settings for that channel to OpenBCI
    if (!cyton.get_isWritingImp()) { //make sure you aren't currently writing imp settings for a channel
      verbosePrint("Writing channel settings for channel " + str(_numChannel+1) + " to OpenBCI!");
      cyton.initChannelWrite(_numChannel);
      channelToWrite = _numChannel;
    }
  }

  public void initImpWrite(int _numChannel, char pORn, char onORoff) {
    verbosePrint("Writing impedance check settings (" + pORn + "," + onORoff +  ") for channel " + str(_numChannel+1) + " to OpenBCI!");
    if (pORn == 'p') {
      impedanceCheckValues[_numChannel][0] = onORoff;
    }
    if (pORn == 'n') {
      impedanceCheckValues[_numChannel][1] = onORoff;
    }
    cyton.writeImpedanceSettings(_numChannel, impedanceCheckValues);
    // impChannelToWrite = _numChannel;
    //after clicking any button, write the new settings for that channel to OpenBCI
    // if (!cyton.get_isWritingChannel()) { //make sure you aren't currently writing imp settings for a channel
      // if you're not currently writing a channel and not waiting to rewrite after you've finished mashing the button
      // if (!cyton.get_isWritingImp() && rewriteImpedanceWhenDoneWriting == false) {
      //   verbosePrint("Writing impedance check settings (" + pORn + "," + onORoff +  ") for channel " + str(_numChannel+1) + " to OpenBCI!");
      //   if (pORn == 'p') {
      //     impedanceCheckValues[_numChannel][0] = onORoff;
      //   }
      //   if (pORn == 'n') {
      //     impedanceCheckValues[_numChannel][1] = onORoff;
      //   }
      //   cyton.initImpWrite(_numChannel);
      //   impChannelToWrite = _numChannel;
      // } else { //else wait until a the current write has finished and then write again ... this is to not overwrite the wrong values while writing a channel
      //   verbosePrint("CONGRATULATIONS, YOU'RE MASHING BUTTONS!");
      //   rewriteImpedanceWhenDoneWriting = true;
      //   impChannelToWriteWhenDoneWriting = _numChannel;
      //
      //   if (pORn == 'p') {
      //     final_pORn = 'p';
      //   }
      //   if (pORn == 'n') {
      //     final_pORn = 'n';
      //   }
      //   final_onORoff = onORoff;
      // }
    // }
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
            cyton.writeChannelSettings(i, channelSettingValues);
            // if you're not currently writing a channel and not waiting to rewrite after you've finished mashing the button
            // if (!cyton.get_isWritingChannel() && rewriteChannelWhenDoneWriting == false) { AJ KEller
            // if (rewriteChannelWhenDoneWriting == false) {
            //   initChannelWrite(i);//write new ADS1299 channel row values to OpenBCI
            // } else { //else wait until a the current write has finished and then write again ... this is to not overwrite the wrong values while writing a channel
            //   verbosePrint("CONGRATULATIONS, YOU'RE MASHING BUTTONS!");
            //   rewriteChannelWhenDoneWriting = true;
            //   channelToWriteWhenDoneWriting = i;
            // }
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
  //   if (eegDataSource == DATASOURCE_CYTON) {
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
  //     if (eegDataSource == DATASOURCE_CYTON) {
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
  //           if (!cyton.get_isWritingChannel() && rewriteChannelWhenDoneWriting == false) {
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  This file contains all key commands for interactivity with GUI & OpenBCI
//  Created by Chip Audette, Joel Murphy, & Conor Russomanno
//  - Extracted from OpenBCI_GUI because it was getting too klunky
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
  //println("OpenBCI_GUI: keyPressed: key = " + key + ", int(key) = " + int(key) + ", keyCode = " + keyCode);

  if(!controlPanel.isOpen && !isNetworkingTextActive()){ //don't parse the key if the control panel is open
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
    case ',':
      //drawContainers = !drawContainers;
      println("Start Marker"); //@@@@@
      //boolean test = isNetworkingTextActive();
      //String sendString = "`\n ";
      //hub.sendCommand(sendString);
      marker_indicater = 1;
      break;
    case '.':
      println("End Marker"); //@@@@@
      //boolean test = isNetworkingTextActive();
      //String sendString1 = "`d";
      //String sendString1 = "`" + char(10);
      //hub.sendCommand(sendString1);
      marker_indicater = 0;
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
    case ':':
      println("test..."); //@@@@@
      boolean test = isNetworkingTextActive();
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
      println("cyton: " + cyton);
      break;

    case '?':
      cyton.printRegisters();
      break;

    case 'd':
      verbosePrint("Updating GUI's channel settings to default...");
      // gui.cc.loadDefaultChannelSettings();
      w_timeSeries.hsc.loadDefaultChannelSettings();
      //cyton.serial_openBCI.write('d');
      cyton.configureAllChannelsToDefault();
      break;
      
    case 'p':
      trainingMode = !trainingMode;
      break;
    case 'P':
      training = !training;
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


    case 'm':
     String picfname = "OpenBCI-" + getDateString() + ".jpg";
     println("OpenBCI_GUI: 'm' was pressed...taking screenshot:" + picfname);
     saveFrame("./SavedData/" + picfname);    // take a shot of that!
     break;

    default:
      if (eegDataSource == DATASOURCE_CYTON) {
        println("Interactivity: '" + key + "' Pressed...sending to Cyton...");
        cyton.write(key);
      } else if (eegDataSource == DATASOURCE_GANGLION) {
        println("Interactivity: '" + key + "' Pressed...sending to Ganglion...");
        hub.sendCommand(key);
      }
      break;
  }
}

public void parseKeycode(int val) {
  //assumes that val is Java keyCode
  switch (val) {
    case 8:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received BACKSPACE keypress.  Ignoring...");
      break;
    case 9:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received TAB keypress.  Ignoring...");
      //gui.showImpedanceButtons = !gui.showImpedanceButtons;
      // gui.incrementGUIpage(); //deprecated with new channel controller
      break;
    case 10:
      println("Enter was pressed.");
      drawPresentation = !drawPresentation;
      break;
    case 16:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received SHIFT keypress.  Ignoring...");
      break;
    case 17:
      //println("OpenBCI_GUI: parseKeycode(" + val + "): received CTRL keypress.  Ignoring...");
      break;
    case 18:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received ALT keypress.  Ignoring...");
      break;
    case 20:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received CAPS LOCK keypress.  Ignoring...");
      break;
    case 27:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received ESC keypress.  Stopping OpenBCI...");
      //stopRunning();
      break;
    case 33:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received PAGE UP keypress.  Ignoring...");
      break;
    case 34:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received PAGE DOWN keypress.  Ignoring...");
      break;
    case 35:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received END keypress.  Ignoring...");
      break;
    case 36:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received HOME keypress.  Ignoring...");
      break;
    case 37:
      if (millis() - myPresentation.timeOfLastSlideChange >= 250) {
        if(myPresentation.currentSlide >= 0){
          myPresentation.slideBack();
          myPresentation.timeOfLastSlideChange = millis();
        }
      }
      break;
    case 38:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received UP ARROW keypress.  Ignoring...");
      dataProcessing_user.switchesActive = true;
      break;
    case 39:
      if (millis() - myPresentation.timeOfLastSlideChange >= 250) {
        if(myPresentation.currentSlide < myPresentation.presentationSlides.length - 1){
          myPresentation.slideForward();
          myPresentation.timeOfLastSlideChange = millis();
        }
      }
      break;
    case 40:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received DOWN ARROW keypress.  Ignoring...");
      dataProcessing_user.switchesActive = false;
      break;
    case 112:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F1 keypress.  Ignoring...");
      break;
    case 113:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F2 keypress.  Ignoring...");
      break;
    case 114:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F3 keypress.  Ignoring...");
      break;
    case 115:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F4 keypress.  Ignoring...");
      break;
    case 116:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F5 keypress.  Ignoring...");
      break;
    case 117:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F6 keypress.  Ignoring...");
      break;
    case 118:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F7 keypress.  Ignoring...");
      break;
    case 119:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F8 keypress.  Ignoring...");
      break;
    case 120:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F9 keypress.  Ignoring...");
      break;
    case 121:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F10 keypress.  Ignoring...");
      break;
    case 122:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F11 keypress.  Ignoring...");
      break;
    case 123:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received F12 keypress.  Ignoring...");
      break;
    case 127:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received DELETE keypress.  Ignoring...");
      break;
    case 155:
      println("OpenBCI_GUI: parseKeycode(" + val + "): received INSERT keypress.  Ignoring...");
      break;
    default:
      println("OpenBCI_GUI: parseKeycode(" + val + "): value is not known.  Ignoring...");
      break;
  }
}

public void mouseDragged() {

  if (systemMode >= SYSTEMMODE_POSTINIT) {

    //calling mouse dragged inly outside of Control Panel
    if (controlPanel.isOpen == false) {
      wm.mouseDragged();
    }
  }
}
//swtich yard if a click is detected
public void mousePressed() {

  // verbosePrint("OpenBCI_GUI: mousePressed: mouse pressed");
  // println("systemMode" + systemMode);
  // controlPanel.CPmousePressed();

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
      //   println("OpenBCI_GUI: FFT data point: " + String.format("%4.2f", dataPoint.x) + " " + dataPoint.x_units + ", " + String.format("%4.2f", dataPoint.y) + " " + dataPoint.y_units);
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
        println("OpenBCI_GUI: mousePressed: clicked in CP box");
        controlPanel.CPmousePressed();
      }
      //if clicked out of panel
      else {
        println("OpenBCI_GUI: mousePressed: outside of CP clicked");
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
    println("OpenBCI_GUI: mouseReleased: screen has been resized...");
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

    //send some info to the HelpButtonText object to be drawn last in OpenBCI_GUI.pde ... we want to make sure it is render last, and on top of all other GUI stuff
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

public void toggleFrameRate(){
  if(frameRateCounter<3){
    frameRateCounter++;
  } else {
    frameRateCounter = 1; // until we resolve the latency issue with 24hz, only allow 30hz minimum (aka frameRateCounter = 1)
  }
  if(frameRateCounter==0){
    frameRate(24); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
    topNav.fpsButton.setString("24 fps");
  }
  if(frameRateCounter==1){
    frameRate(30); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
    topNav.fpsButton.setString("30 fps");
  }
  if(frameRateCounter==2){
    frameRate(45); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
    topNav.fpsButton.setString("45 fps");
  }
  if(frameRateCounter==3){
    frameRate(60); //refresh rate ... this will slow automatically, if your processor can't handle the specified rate
    topNav.fpsButton.setString("60 fps");
  }
}

public boolean isNetworkingTextActive(){
  boolean isAFieldActive = false;
  if (w_networking != null) {
    int numTextFields = w_networking.cp5_networking.getAll(Textfield.class).size();
    for(int i = 0; i < numTextFields; i++){
      if(w_networking.cp5_networking.getAll(Textfield.class).get(i).isFocus()){
        isAFieldActive = true;
      }
    }
  }
  // println("Test - " + w_networking.cp5_networking.getAll(Textfield.class)); //loop through networking textfields and find out if any of the are active

  //isFocus(); returns true if active for textField...
  println(isAFieldActive);
  return isAFieldActive; //if not, return false
}

boolean highDPI = false;
public void toggleHighDPI(){
  highDPI = !highDPI;
  println("High DPI? " + highDPI);
}
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

boolean werePacketsDroppedHub = false;
int numPacketsDroppedHub = 0;

public void clientEvent(Client someClient) {
  // print("Server Says:  ");
  int p = hub.tcpBufferPositon;
  hub.tcpBuffer[p] = hub.tcpClient.readChar();
  hub.tcpBufferPositon++;

  if(p > 2) {
    String posMatch  = new String(hub.tcpBuffer, p - 2, 3);
    if (posMatch.equals(hub.TCP_STOP)) {
      if (!hub.nodeProcessHandshakeComplete) {
        hub.nodeProcessHandshakeComplete = true;
        hub.setHubIsRunning(true);
        println("Hub: clientEvent: handshake complete");
      }
      // Get a string from the tcp buffer
      String msg = new String(hub.tcpBuffer, 0, p);
      // Send the new string message to be processed

      if (eegDataSource == DATASOURCE_GANGLION) {
        hub.parseMessage(msg);
        // Check to see if the ganglion ble list needs to be updated
        if (hub.deviceListUpdated) {
          hub.deviceListUpdated = false;
          if (ganglion.isBLE()) {
            controlPanel.bleBox.refreshBLEList();
          } else {
            controlPanel.wifiBox.refreshWifiList();
          }
        }
      } else if (eegDataSource == DATASOURCE_CYTON) {
        // Do stuff for cyton
        hub.parseMessage(msg);
        // Check to see if the ganglion ble list needs to be updated
        if (hub.deviceListUpdated) {
          hub.deviceListUpdated = false;
          controlPanel.wifiBox.refreshWifiList();
        }
      }

      // Reset the buffer position
      hub.tcpBufferPositon = 0;
    }
  }
}

class Hub {
  final static String TCP_CMD_ACCEL = "a";
  final static String TCP_CMD_BOARD_TYPE = "b";
  final static String TCP_CMD_CONNECT = "c";
  final static String TCP_CMD_COMMAND = "k";
  final static String TCP_CMD_DISCONNECT = "d";
  final static String TCP_CMD_DATA = "t";
  final static String TCP_CMD_ERROR = "e";
  final static String TCP_CMD_EXAMINE = "x";
  final static String TCP_CMD_IMPEDANCE = "i";
  final static String TCP_CMD_LOG = "l";
  final static String TCP_CMD_PROTOCOL = "p";
  final static String TCP_CMD_SCAN = "s";
  final static String TCP_CMD_SD = "m";
  final static String TCP_CMD_STATUS = "q";
  final static String TCP_CMD_WIFI = "w";
  final static String TCP_STOP = ",;\n";

  final static String TCP_ACTION_START = "start";
  final static String TCP_ACTION_STATUS = "status";
  final static String TCP_ACTION_STOP = "stop";

  final static String TCP_WIFI_ERASE_CREDENTIALS = "eraseCredentials";
  final static String TCP_WIFI_GET_FIRMWARE_VERSION = "getFirmwareVersion";
  final static String TCP_WIFI_GET_IP_ADDRESS = "getIpAddress";
  final static String TCP_WIFI_GET_MAC_ADDRESS = "getMacAddress";
  final static String TCP_WIFI_GET_TYPE_OF_ATTACHED_BOARD = "getTypeOfAttachedBoard";

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
  final static int RESP_ERROR_ALREADY_CONNECTED = 408;
  final static int RESP_ERROR_BAD_PACKET = 500;
  final static int RESP_ERROR_BAD_NOBLE_START = 501;
  final static int RESP_ERROR_CHANNEL_SETTINGS = 423;
  final static int RESP_ERROR_CHANNEL_SETTINGS_SYNC_IN_PROGRESS = 422;
  final static int RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_SET_CHANNEL = 424;
  final static int RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_PARSE = 425;
  final static int RESP_ERROR_COMMAND_NOT_ABLE_TO_BE_SENT = 406;
  final static int RESP_ERROR_COMMAND_NOT_RECOGNIZED = 434;
  final static int RESP_ERROR_DEVICE_NOT_FOUND = 405;
  final static int RESP_ERROR_IMPEDANCE_COULD_NOT_START = 414;
  final static int RESP_ERROR_IMPEDANCE_COULD_NOT_STOP = 415;
  final static int RESP_ERROR_IMPEDANCE_FAILED_TO_SET_IMPEDANCE = 430;
  final static int RESP_ERROR_IMPEDANCE_FAILED_TO_PARSE = 431;
  final static int RESP_ERROR_NO_OPEN_BLE_DEVICE = 400;
  final static int RESP_ERROR_UNABLE_TO_CONNECT = 402;
  final static int RESP_ERROR_UNABLE_TO_DISCONNECT = 401;
  final static int RESP_ERROR_PROTOCOL_UNKNOWN = 418;
  final static int RESP_ERROR_PROTOCOL_BLE_START = 419;
  final static int RESP_ERROR_PROTOCOL_NOT_STARTED = 420;
  final static int RESP_ERROR_UNABLE_TO_SET_BOARD_TYPE = 421;
  final static int RESP_ERROR_SCAN_ALREADY_SCANNING = 409;
  final static int RESP_ERROR_SCAN_NONE_FOUND = 407;
  final static int RESP_ERROR_SCAN_NO_SCAN_TO_STOP = 410;
  final static int RESP_ERROR_SCAN_COULD_NOT_START = 412;
  final static int RESP_ERROR_SCAN_COULD_NOT_STOP = 411;
  final static int RESP_ERROR_TIMEOUT_SCAN_STOPPED = 432;
  final static int RESP_ERROR_WIFI_ACTION_NOT_RECOGNIZED = 427;
  final static int RESP_ERROR_WIFI_COULD_NOT_ERASE_CREDENTIALS = 428;
  final static int RESP_ERROR_WIFI_COULD_NOT_SET_LATENCY = 429;
  final static int RESP_ERROR_WIFI_NEEDS_UPDATE = 435;
  final static int RESP_ERROR_WIFI_NOT_CONNECTED = 426;
  final static int RESP_GANGLION_FOUND = 201;
  final static int RESP_SUCCESS = 200;
  final static int RESP_SUCCESS_DATA_ACCEL = 202;
  final static int RESP_SUCCESS_DATA_IMPEDANCE = 203;
  final static int RESP_SUCCESS_DATA_SAMPLE = 204;
  final static int RESP_WIFI_FOUND = 205;
  final static int RESP_SUCCESS_CHANNEL_SETTING = 207;
  final static int RESP_STATUS_CONNECTED = 300;
  final static int RESP_STATUS_DISCONNECTED = 301;
  final static int RESP_STATUS_SCANNING = 302;
  final static int RESP_STATUS_NOT_SCANNING = 303;

  final static int LATENCY_5_MS = 5000;
  final static int LATENCY_10_MS = 10000;
  final static int LATENCY_20_MS = 20000;

  final static String TCP = "tcp";
  final static String UDP = "udp";
  final static String UDP_BURST = "udpBurst";

  public int curLatency = LATENCY_10_MS;

  public String[] deviceList = new String[0];
  public boolean deviceListUpdated = false;

  private int bleErrorCounter = 0;
  private int prevSampleIndex = 0;

  private int requestedSampleRate = 0;
  private boolean setSampleRate = false;

  private int state = STATE_NOCOM;
  int prevState_millis = 0; // Used for calculating connect time out

  private int nEEGValuesPerPacket = 8;
  private int nAuxValuesPerPacket = 3;

  private int tcpHubPort = 10996;
  private String tcpHubIP = "127.0.0.1";
  private String tcpHubFull = tcpHubIP + ":" + tcpHubPort;
  private boolean tcpClientActive = false;
  private int tcpTimeout = 1000;

  private DataPacket_ADS1299 dataPacket;

  public Client tcpClient;
  private boolean portIsOpen = false;
  private boolean connected = false;

  public int numberOfDevices = 0;
  public int maxNumberOfDevices = 10;
  private boolean hubRunning = false;
  public char[] tcpBuffer = new char[4096];
  public int tcpBufferPositon = 0;
  private String curProtocol = PROTOCOL_WIFI;
  private String curInternetProtocol = TCP;

  private boolean waitingForResponse = false;
  private boolean nodeProcessHandshakeComplete = false;
  private boolean searching = false;
  public boolean shouldStartNodeApp = false;
  private boolean checkingImpedance = false;
  private boolean connectForWifiConfig = false;
  private boolean accelModeActive = false;
  private boolean newAccelData = false;
  public int[] accelArray = new int[NUM_ACCEL_DIMS];
  public int[] validAccelValues = {0, 0, 0};
  public int validLastMarker;
  public boolean validNewAccelData = false;

  public boolean impedanceUpdated = false;
  public int[] impedanceArray = new int[NCHAN_GANGLION + 1];

  // Getters
  public int get_state() { return state; }
  public int getLatency() { return curLatency; }
  public String getWifiInternetProtocol() { return curInternetProtocol; }
  public boolean isPortOpen() { return portIsOpen; }
  public boolean isHubRunning() { return hubRunning; }
  public boolean isSearching() { return searching; }
  public boolean isCheckingImpedance() { return checkingImpedance; }
  public boolean isAccelModeActive() { return accelModeActive; }
  public void setLatency(int latency) {
    curLatency = latency;
    output("Setting Latency to " + latency);
    println("Setting Latency Protocol to " + latency);
  }
  public void setWifiInternetProtocol(String internetProtocol) {
    curInternetProtocol = internetProtocol;
    output("Setting WiFi Internet Protocol to " + internetProtocol);
    println("Setting WiFi Internet Protocol to " + internetProtocol);
  }

  private PApplet mainApplet;

  //constructors
  Hub() {};  //only use this if you simply want access to some of the constants
  Hub(PApplet applet) {
    mainApplet = applet;

    // Able to start tcpClient connection?
    startTCPClient(mainApplet);

  }

  public void initDataPackets(int _nEEGValuesPerPacket, int _nAuxValuesPerPacket) {
    nEEGValuesPerPacket = _nEEGValuesPerPacket;
    nAuxValuesPerPacket = _nAuxValuesPerPacket;
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
      tcpClient = new Client(applet, tcpHubIP, tcpHubPort);
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
      write(TCP_CMD_STATUS + TCP_STOP);
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
      case 'b': // board type setting
        processBoardType(msg);
        break;
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
        int code = PApplet.parseInt(list[1]);
        println("Hub: parseMessage: error: " + list[2]);
        if (code == RESP_ERROR_COMMAND_NOT_RECOGNIZED) {
          output("Hub in data folder outdated. Download a new hub for your OS at https://github.com/OpenBCI/OpenBCI_Ganglion_Electron/releases/latest");
        }
        break;
      case 'x':
        processExamine(msg);
        break;
      case 's': // Scan
        processScan(msg);
        break;
      case 'l':
        println("Hub: Log: " + list[1]);
        break;
      case 'p':
        processProtocol(msg);
        break;
      case 'q':
        processStatus(msg);
        break;
      case 'r':
        processRegisterQuery(msg);
        break;
      case 'm':
        processSDCard(msg);
        break;
      case 'w':
        processWifi(msg);
        break;
      case 'k':
        processCommand(msg);
        break;
      default:
        println("Hub: parseMessage: default: " + msg);
        output("Hub in data folder outdated. Download a new hub for your OS at https://github.com/OpenBCI/OpenBCI_Ganglion_Electron/releases/latest");
        break;
    }
  }

  private void handleError(int code, String msg) {
    output("Code " + code + " Error: " + msg);
    println("Code " + code + " Error: " + msg);
  }

  public void setBoardType(String boardType) {
    println("Hub: setBoardType(): sending \'" + boardType + " -- " + millis());
    write(TCP_CMD_BOARD_TYPE + "," + boardType + TCP_STOP);
  }

  private void processBoardType(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_SUCCESS:
        if (sdSetting > 0) {
          println("Hub: processBoardType: success, starting SD card now -- " + millis());
          sdCardStart(sdSetting);
        } else {
          println("Hub: processBoardType: success -- " + millis());
          initAndShowGUI();
        }
        break;
      case RESP_ERROR_UNABLE_TO_SET_BOARD_TYPE:
      default:
        killAndShowMsg(list[2]);
        break;
    }
  }

  private void processConnect(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    println("Hub: processConnect: made it -- " + millis() + " code: " + code);
    switch (code) {
      case RESP_SUCCESS:
      case RESP_ERROR_ALREADY_CONNECTED:
        changeState(STATE_SYNCWITHHARDWARE);
        if (eegDataSource == DATASOURCE_CYTON) {
          if (nchan == 8) {
            setBoardType("cyton");
          } else {
            setBoardType("daisy");
          }
        } else {
          println("Hub: parseMessage: connect: success! -- " + millis());
          initAndShowGUI();
        }
        break;
      case RESP_ERROR_UNABLE_TO_CONNECT:
        println("Error in processConnect: RESP_ERROR_UNABLE_TO_CONNECT");
        if (list[2].equals("Error: Invalid sample rate")) {
          if (eegDataSource == DATASOURCE_CYTON) {
            killAndShowMsg("WiFi Shield is connected to a Ganglion. Please select LIVE (from Ganglion) instead of LIVE (from Cyton)");
          } else {
            killAndShowMsg("WiFi Shield is connected to a Cyton. Please select LIVE (from Cyton) instead LIVE (from Cyton)");
          }
        } else {
          killAndShowMsg(list[2]);
        }
        break;
      case RESP_ERROR_WIFI_NEEDS_UPDATE:
        println("Error in processConnect: RESP_ERROR_WIFI_NEEDS_UPDATE");
        killAndShowMsg("WiFi Shield Firmware is out of date. Learn to update: docs.openbci.com/Hardware/12-Wifi_Programming_Tutorial");
        break;
      default:
        println("Error in processConnect");
        handleError(code, list[2]);
        break;
    }
  }

  private void processExamine(String msg) {
    // println(msg);
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_SUCCESS:
        portIsOpen = true;
        output("Connected to WiFi Shield named " + wifi_portName);
        if (wcBox.isShowing) {
          wcBox.updateMessage("Connected to WiFi Shield named " + wifi_portName);
        }
        break;
      case RESP_ERROR_ALREADY_CONNECTED:
        portIsOpen = true;
        output("WiFi Shield is still connected to " + wifi_portName);
        break;
      case RESP_ERROR_UNABLE_TO_CONNECT:
        output("No WiFi Shield found, visit docs.openbci.com/Tutorials/03-Wifi_Getting_Started_Guide to learn how to connect.");
        break;
      default:
        if (wcBox.isShowing) println("it is showing"); //controlPanel.hideWifiPopoutBox();
        handleError(code, list[2]);
        break;
    }
  }

  private void initAndShowGUI() {
    changeState(STATE_NORMAL);
    systemMode = SYSTEMMODE_POSTINIT;
    controlPanel.close();
    topNav.controlPanelCollapser.setIsActive(false);
    outputSuccess("The GUI is done intializing. Press \"Start Data Stream\" to start streaming!");
    portIsOpen = true;
    controlPanel.hideAllBoxes();
  }

  private void killAndShowMsg(String msg) {
    abandonInit = true;
    initSystemButton.setString("START SYSTEM");
    controlPanel.open();
    outputError(msg);
    portIsOpen = false;
    haltSystem();
  }

  /**
   * @description Sends a command to ganglion board
   */
  public void sendCommand(char c) {
    println("Hub: sendCommand(char): sending \'" + c + "\'");
    write(TCP_CMD_COMMAND + "," + c + TCP_STOP);
  }

  /**
   * @description Sends a command to ganglion board
   */
  public void sendCommand(String s) {
    println("Hub: sendCommand(String): sending \'" + s + "\'");
    write(TCP_CMD_COMMAND + "," + s + TCP_STOP);
  }

  public void processCommand(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_SUCCESS:
        println("Hub: processCommand: success -- " + millis());
        break;
      case RESP_ERROR_COMMAND_NOT_ABLE_TO_BE_SENT:
        println("Hub: processCommand: ERROR_COMMAND_NOT_ABLE_TO_BE_SENT -- " + millis() + " " + list[2]);
        break;
      case RESP_ERROR_PROTOCOL_NOT_STARTED:
        println("Hub: processCommand: RESP_ERROR_PROTOCOL_NOT_STARTED -- " + millis() + " " + list[2]);
        break;
      default:
        break;
    }
  }

  public void processAccel(String msg) {
    String[] list = split(msg, ',');
    if (Integer.parseInt(list[1]) == RESP_SUCCESS_DATA_ACCEL) {
      for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
        accelArray[i] = Integer.parseInt(list[i + 2]);
      }
      newAccelData = true;
      if (accelArray[0] > 0 || accelArray[1] > 0 || accelArray[2] > 0) {
        for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
          validAccelValues[i] = accelArray[i];
        }
      }
    }
  }

  public void processData(String msg) {
    try {
      // println(msg);
      String[] list = split(msg, ',');
      int code = Integer.parseInt(list[1]);
      int stopByte = 0xC0;
      if ((eegDataSource == DATASOURCE_GANGLION || eegDataSource == DATASOURCE_CYTON) && systemMode == 10 && isRunning) {
        if (Integer.parseInt(list[1]) == RESP_SUCCESS_DATA_SAMPLE) {
          // Sample number stuff
          dataPacket.sampleIndex = PApplet.parseInt(Integer.parseInt(list[2]));

          if ((dataPacket.sampleIndex - prevSampleIndex) != 1) {
            if(dataPacket.sampleIndex != 0){  // if we rolled over, don't count as error
              bleErrorCounter++;

              werePacketsDroppedHub = true; //set this true to activate packet duplication in serialEvent
              if(dataPacket.sampleIndex < prevSampleIndex){   //handle the situation in which the index jumps from 250s past 255, and back to 0
                numPacketsDroppedHub = (dataPacket.sampleIndex+(curProtocol == PROTOCOL_BLE ? 200 : 255)) - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
              } else {
                numPacketsDroppedHub = dataPacket.sampleIndex - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
              }
              println("Hub: apparent sampleIndex jump from Serial data: " + prevSampleIndex + " to  " + dataPacket.sampleIndex + ".  Keeping packet. (" + bleErrorCounter + ")");
              println("numPacketsDropped = " + numPacketsDroppedHub);
            }
          }
          prevSampleIndex = dataPacket.sampleIndex;

          // Channel data storage
          for (int i = 0; i < nEEGValuesPerPacket; i++) {
            dataPacket.values[i] = Integer.parseInt(list[3 + i]);
          }
          if (newAccelData) {
            newAccelData = false;
            for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
              dataPacket.auxValues[i] = accelArray[i];
              dataPacket.rawAuxValues[i][0] = PApplet.parseByte(accelArray[i]);
            }
          } else {
            if (list.length > nEEGValuesPerPacket + 5) {
              int valCounter = nEEGValuesPerPacket + 3;
              // println(list[valCounter]);
              stopByte = Integer.parseInt(list[valCounter++]);
              int valsToRead = list.length - valCounter - 1;
              // println(msg);
              // println("stopByte: " + stopByte + " valCounter: " + valCounter + " valsToRead: " + valsToRead);
              if (stopByte == 0xC0) {
                for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
                  accelArray[i] = Integer.parseInt(list[valCounter++]);
                  dataPacket.auxValues[i] = accelArray[i];
                  dataPacket.rawAuxValues[i][0] = PApplet.parseByte(accelArray[i]);
                  dataPacket.rawAuxValues[i][1] = PApplet.parseByte(accelArray[i] >> 8);
                }
                if (accelArray[0] > 0 || accelArray[1] > 0 || accelArray[2] > 0) {
                  // println(msg);
                  for (int i = 0; i < NUM_ACCEL_DIMS; i++) {
                    validAccelValues[i] = accelArray[i];
                  }
                }
              } else {
                if (valsToRead == 6) {
                  for (int i = 0; i < 3; i++) {
                    // println(list[valCounter]);
                    int val1 = Integer.parseInt(list[valCounter++]);
                    int val2 = Integer.parseInt(list[valCounter++]);

                    dataPacket.auxValues[i] = (val1 << 8) | val2;
                    validAccelValues[i] = (val1 << 8) | val2;

                    dataPacket.rawAuxValues[i][0] = PApplet.parseByte(val2);
                    dataPacket.rawAuxValues[i][1] = PApplet.parseByte(val1 << 8);
                  }
                  // println(validAccelValues[1]);
                }
              }
            }
          }
          getRawValues(dataPacket);
          // println(binary(dataPacket.values[0], 24) + '\n' + binary(dataPacket.rawValues[0][0], 8) + binary(dataPacket.rawValues[0][1], 8) + binary(dataPacket.rawValues[0][2], 8) + '\n');
          // println(dataPacket.values[7]);
          curDataPacketInd = (curDataPacketInd+1) % dataPacketBuff.length; // This is also used to let the rest of the code that it may be time to do something
          copyDataPacketTo(dataPacketBuff[curDataPacketInd]);

          // KILL SPIKES!!!
          // if(werePacketsDroppedHub){
          //   // println("Packets Dropped ... doing some stuff...");
          //   for(int i = numPacketsDroppedHub; i > 0; i--){
          //     int tempDataPacketInd = curDataPacketInd - i; //
          //     if(tempDataPacketInd >= 0 && tempDataPacketInd < dataPacketBuff.length){
          //       // println("i = " + i);
          //       copyDataPacketTo(dataPacketBuff[tempDataPacketInd]);
          //     } else {
          //       if (eegDataSource == DATASOURCE_GANGLION) {
          //         copyDataPacketTo(dataPacketBuff[tempDataPacketInd+200]);
          //       } else {
          //         copyDataPacketTo(dataPacketBuff[tempDataPacketInd+255]);
          //       }
          //     }
          //     //put the last stored packet in # of packets dropped after that packet
          //   }
          //
          //   //reset werePacketsDropped & numPacketsDropped
          //   werePacketsDroppedHub = false;
          //   numPacketsDroppedHub = 0;
          // }

          switch (outputDataSource) {
            case OUTPUT_SOURCE_ODF:
              if (eegDataSource == DATASOURCE_GANGLION) {
                fileoutput_odf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd], ganglion.get_scale_fac_uVolts_per_count(), ganglion.get_scale_fac_accel_G_per_count(), stopByte);
              } else {
                fileoutput_odf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd], cyton.get_scale_fac_uVolts_per_count(), cyton.get_scale_fac_accel_G_per_count(), stopByte);
              }
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
          println("Hub: parseMessage: data: bad");
        }
      }
    } catch (Exception e) {
      print("\n\n");
      println(msg);
      println("Hub: parseMessage: error: " + e);
      e.printStackTrace();
    }

  }

  private void processDisconnect(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_SUCCESS:
        if (!waitingForResponse) {
          if (eegDataSource == DATASOURCE_CYTON) {
            killAndShowMsg("Dang! Lost connection to Cyton. Please move closer or get a new battery!");
          } else {
            killAndShowMsg("Dang! Lost connection to Ganglion. Please move closer or get a new battery!");
          }
        } else {
          waitingForResponse = false;
        }
        break;
      case RESP_ERROR_UNABLE_TO_DISCONNECT:
        break;
    }
    portIsOpen = false;
  }

  private void processImpedance(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_ERROR_IMPEDANCE_COULD_NOT_START:
        ganglion.overrideCheckingImpedance(false);
      case RESP_ERROR_IMPEDANCE_COULD_NOT_STOP:
      case RESP_ERROR_IMPEDANCE_FAILED_TO_SET_IMPEDANCE:
      case RESP_ERROR_IMPEDANCE_FAILED_TO_PARSE:
        handleError(code, list[2]);
        break;
      case RESP_SUCCESS_DATA_IMPEDANCE:
        ganglion.processImpedance(msg);
        break;
      case RESP_SUCCESS:
        output("Success: Impedance " + list[2] + ".");
        break;
      default:
        handleError(code, list[2]);
        break;
    }
  }

  private void processProtocol(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_SUCCESS:
        output("Transfer Protocol set to " + list[2]);
        println("Transfer Protocol set to " + list[2]);
        if (eegDataSource == DATASOURCE_GANGLION && ganglion.isBLE()) {
          hub.searchDeviceStart();
          outputInfo("BLE was powered up sucessfully, now searching for BLE devices.");
        }
        break;
      case RESP_ERROR_PROTOCOL_BLE_START:
        outputError("Failed to start Ganglion BLE Driver, please see http://docs.openbci.com/Tutorials/02-Ganglion_Getting%20Started_Guide");
        println("Failed to start Ganglion BLE Driver, please see http://docs.openbci.com/Tutorials/02-Ganglion_Getting%20Started_Guide");
        break;
      default:
        handleError(code, list[2]);
        break;
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
      println("Hub: processStatus: Problem in the Hub");
      output("Problem starting Ganglion Hub. Please make sure compatible USB is configured, then restart this GUI.");
    } else {
      println("Hub: processStatus: Started Successfully");
    }
  }

  private void processRegisterQuery(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);

    switch (code) {
      case RESP_ERROR_CHANNEL_SETTINGS:
        killAndShowMsg("Failed to sync with Cyton, please power cycle your dongle and board.");
        println("RESP_ERROR_CHANNEL_SETTINGS general error: " + list[2]);
        break;
      case RESP_ERROR_CHANNEL_SETTINGS_SYNC_IN_PROGRESS:
        println("tried to sync channel settings but there was already one in progress");
        break;
      case RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_SET_CHANNEL:
        println("an error was thrown trying to set the channels | error: " + list[2]);
        break;
      case RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_PARSE:
        println("an error was thrown trying to call the function to set the channels | error: " + list[2]);
        break;
      case RESP_SUCCESS:
        // Sent when either a scan was stopped or started Successfully
        String action = list[2];
        switch (action) {
          case TCP_ACTION_START:
            println("Query registers for cyton channel settings");
            break;
        }
        break;
      case RESP_SUCCESS_CHANNEL_SETTING:
        int channelNumber = Integer.parseInt(list[2]);
        // power down comes in as either 'true' or 'false', 'true' is a '1' and false is a '0'
        channelSettingValues[channelNumber][0] = list[3].equals("true") ? '1' : '0';
        // gain comes in as an int, either 1, 2, 4, 6, 8, 12, 24 and must get converted to
        //  '0', '1', '2', '3', '4', '5', '6' respectively, of course.
        channelSettingValues[channelNumber][1] = cyton.getCommandForGain(Integer.parseInt(list[4]));
        // input type comes in as a string version and must get converted to char
        channelSettingValues[channelNumber][2] = cyton.getCommandForInputType(list[5]);
        // bias is like power down
        channelSettingValues[channelNumber][3] = list[6].equals("true") ? '1' : '0';
        // srb2 is like power down
        channelSettingValues[channelNumber][4] = list[7].equals("true") ? '1' : '0';
        // srb1 is like power down
        channelSettingValues[channelNumber][5] = list[8].equals("true") ? '1' : '0';
        break;
    }
  }

  private void processScan(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_GANGLION_FOUND:
      case RESP_WIFI_FOUND:
        // Sent every time a new ganglion device is found
        if (searchDeviceAdd(list[2])) {
          deviceListUpdated = true;
        }
        break;
      case RESP_ERROR_SCAN_ALREADY_SCANNING:
        // Sent when a start send command is sent and the module is already
        //  scanning.
        // handleError(code, list[2]);
        searching = true;
        break;
      case RESP_SUCCESS:
        // Sent when either a scan was stopped or started Successfully
        String action = list[2];
        switch (action) {
          case TCP_ACTION_START:
            searching = true;
            break;
          case TCP_ACTION_STOP:
            searching = false;
            break;
        }
        break;
      case RESP_ERROR_TIMEOUT_SCAN_STOPPED:
        searching = false;
        break;
      case RESP_ERROR_SCAN_COULD_NOT_START:
        // Sent when err on search start
        handleError(code, list[2]);
        searching = false;
        break;
      case RESP_ERROR_SCAN_COULD_NOT_STOP:
        // Send when err on search stop
        handleError(code, list[2]);
        searching = false;
        break;
      case RESP_STATUS_SCANNING:
        // Sent when after status action sent to node and module is searching
        searching = true;
        break;
      case RESP_STATUS_NOT_SCANNING:
        // Sent when after status action sent to node and module is NOT searching
        searching = false;
        break;
      case RESP_ERROR_SCAN_NO_SCAN_TO_STOP:
        // Sent when a 'stop' action is sent to node and there is no scan to stop.
        // handleError(code, list[2]);
        searching = false;
        break;
      case RESP_ERROR_UNKNOWN:
      default:
        handleError(code, list[2]);
        break;
    }
  }

  public void sdCardStart(int sdSetting) {
    String sdSettingStr = cyton.getSDSettingForSetting(sdSetting);
    println("Hub: sdCardStart(): sending \'" + sdSettingStr + "\' with value " + sdSetting);
    write(TCP_CMD_SD + "," + TCP_ACTION_START + "," + sdSettingStr + TCP_STOP);
  }

  private void processSDCard(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    String action = list[2];

    switch(code) {
      case RESP_SUCCESS:
        // Sent when either a scan was stopped or started Successfully
        switch (action) {
          case TCP_ACTION_START:
            println("sd card setting set so now attempting to sync channel settings");
            // cyton.syncChannelSettings();
            initAndShowGUI();
            break;
          case TCP_ACTION_STOP:
            println(list[3]);
            break;
        }
        break;
      case RESP_ERROR_UNKNOWN:
        switch (action) {
          case TCP_ACTION_START:
            killAndShowMsg(list[3]);
            break;
          case TCP_ACTION_STOP:
            println(list[3]);
            break;
        }
        break;
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

  public boolean isSuccessCode(int c) {
    return c == RESP_SUCCESS;
  }

  public void updateSyncState(int sdSetting) {
    //has it been 3000 milliseconds since we initiated the serial port? We want to make sure we wait for the OpenBCI board to finish its setup()
    if ( (millis() - prevState_millis > COM_INIT_MSEC) && (prevState_millis != 0) && (state == STATE_COMINIT) ) {
      state = STATE_SYNCWITHHARDWARE;
      timeOfLastCommand = millis();
      // potentialFailureMessage = "";
      // defaultChannelSettings = ""; //clear channel setting string to be reset upon a new Init System
      // daisyOrNot = ""; //clear daisyOrNot string to be reset upon a new Init System
      println("InterfaceHub: systemUpdate: [0] Sending 'v' to OpenBCI to reset hardware in case of 32bit board...");
      // write('v');
    }

    //if we are in SYNC WITH HARDWARE state ... trigger a command
    // if ( (state == STATE_SYNCWITHHARDWARE) && (currentlySyncing == false) ) {
    //   if (millis() - timeOfLastCommand > 200) {
    //     timeOfLastCommand = millis();
    //     // hardwareSyncStep++;
    //     cyton.syncWithHardware(sdSetting);
    //   }
    // }
  }

  public void closePort() {
    switch (curProtocol) {
      case PROTOCOL_BLE:
        disconnectBLE();
        break;
      case PROTOCOL_WIFI:
        disconnectWifi();
        break;
      case PROTOCOL_SERIAL:
        disconnectSerial();
        break;
      default:
        break;
    }
    changeState(STATE_NOCOM);
  }

  // CONNECTION
  public void connectBLE(String id) {
    write(TCP_CMD_CONNECT + "," + id + TCP_STOP);
    verbosePrint("OpenBCI_GUI: hub : Sent connect to Hub - Id: " + id);

  }
  public void disconnectBLE() {
    waitingForResponse = true;
    write(TCP_CMD_DISCONNECT + "," + PROTOCOL_BLE + "," + TCP_STOP);
  }

  public void connectWifi(String id) {
    write(TCP_CMD_CONNECT + "," + id + "," + requestedSampleRate + "," + curLatency + "," + curInternetProtocol + TCP_STOP);
    verbosePrint("OpenBCI_GUI: hub : Sent connect to Hub - Id: " + id + " SampleRate: " + requestedSampleRate + "Hz Latency: " + curLatency + "ms");

  }

  public void examineWifi(String id) {
    write(TCP_CMD_EXAMINE + "," + id + TCP_STOP);
  }

  public int disconnectWifi() {
    waitingForResponse = true;
    write(TCP_CMD_DISCONNECT +  "," + PROTOCOL_WIFI + "," +  TCP_STOP);
    return 0;
  }

  public void connectSerial(String id) {
    waitingForResponse = true;
    write(TCP_CMD_CONNECT + "," + id + TCP_STOP);
    verbosePrint("OpenBCI_GUI: hub : Sent connect to Hub - Id: " + id);
    delay(1000);

  }
  public int disconnectSerial() {
    println("disconnecting serial");
    waitingForResponse = true;
    write(TCP_CMD_DISCONNECT +  "," + PROTOCOL_SERIAL + "," +  TCP_STOP);
    return 0;
  }

  public void setProtocol(String _protocol) {
    curProtocol = _protocol;
    write(TCP_CMD_PROTOCOL + ",start," + curProtocol + TCP_STOP);
  }

  public int getSampleRate() {
    return requestedSampleRate;
  }

  public void setSampleRate(int _sampleRate) {
    requestedSampleRate = _sampleRate;
    setSampleRate = true;
    println("\n\nsample rate set to: " + _sampleRate);
  }

  public void getWifiInfo(String info) {
    write(TCP_CMD_WIFI + "," + info + TCP_STOP);
  }

  public void setWifiInfo(String info, int value) {
    write(TCP_CMD_WIFI + "," + info + "," + value + TCP_STOP);
  }

  private void processWifi(String msg) {
    String[] list = split(msg, ',');
    int code = Integer.parseInt(list[1]);
    switch (code) {
      case RESP_ERROR_WIFI_ACTION_NOT_RECOGNIZED:
        println("Sent an action to hub for wifi info but the command was unrecognized");
        output("Sent an action to hub for wifi info but the command was unrecognized");
        break;
      case RESP_ERROR_WIFI_NOT_CONNECTED:
        println("Tried to get wifi info but no WiFi Shield was connected.");
        output("Tried to get wifi info but no WiFi Shield was connected.");
        break;
      case RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_SET_CHANNEL:
        println("an error was thrown trying to set the channels | error: " + list[2]);
        break;
      case RESP_ERROR_CHANNEL_SETTINGS_FAILED_TO_PARSE:
        println("an error was thrown trying to call the function to set the channels | error: " + list[2]);
        break;
      case RESP_SUCCESS:
        // Sent when either a scan was stopped or started Successfully
        if (wcBox.isShowing) {
          String msgForWcBox = list[3];

          switch (list[2]) {
            case TCP_WIFI_GET_TYPE_OF_ATTACHED_BOARD:
              switch(list[3]) {
                case "none":
                  msgForWcBox = "No OpenBCI Board attached to WiFi Shield";
                  break;
                case "ganglion":
                  msgForWcBox = "4-channel Ganglion attached to WiFi Shield";
                  break;
                case "cyton":
                  msgForWcBox = "8-channel Cyton attached to WiFi Shield";
                  break;
                case "daisy":
                  msgForWcBox = "16-channel Cyton with Daisy attached to WiFi Shield";
                  break;
              }
              break;
            case TCP_WIFI_ERASE_CREDENTIALS:
              output("WiFi credentials have been erased and WiFi Shield is in hotspot mode. If erase fails, remove WiFi Shield from OpenBCI Board.");
              msgForWcBox = "";
              controlPanel.hideWifiPopoutBox();
              wifi_portName = "N/A";
              clearDeviceList();
              controlPanel.wifiBox.refreshWifiList();
              break;
          }
          wcBox.updateMessage(msgForWcBox);
        }
        println("Success for wifi " + list[2] + ": " + list[3]);
        break;
    }
  }

  /**
   * @description Write to TCP server
   * @params out {String} - The string message to write to the server.
   * @returns {boolean} - True if able to write, false otherwise.
   */
  public boolean write(String out) {
    try {
      // println("out " + out);
      tcpClient.write(out);
      return true;
    } catch (Exception e) {
      if (isWindows()) {
        killAndShowMsg("Please start OpenBCIHub before launching this application.");
      } else {
        killAndShowMsg("Hub has crashed, please restart your application.");
      }
      println("Error: Attempted to TCP write with no server connection initialized");
      return false;
    }
  }
  public boolean write(char val) {
    return write(String.valueOf(val));
  }

  public int changeState(int newState) {
    state = newState;
    prevState_millis = millis();
    return 0;
  }

  public void clearDeviceList() {
    deviceList = null;
    numberOfDevices = 0;
  }

  public void searchDeviceStart() {
    clearDeviceList();
    write(TCP_CMD_SCAN + ',' + TCP_ACTION_START + TCP_STOP);
  }

  public void searchDeviceStop() {
    write(TCP_CMD_SCAN + ',' + TCP_ACTION_STOP + TCP_STOP);
  }

  public boolean searchDeviceAdd(String localName) {
    if (numberOfDevices == 0) {
      numberOfDevices++;
      deviceList = new String[numberOfDevices];
      deviceList[0] = localName;
      return true;
    } else {
      boolean willAddToDeviceList = true;
      for (int i = 0; i < numberOfDevices; i++) {
        if (localName.equals(deviceList[i])) {
          willAddToDeviceList = false;
          break;
        }
      }
      if (willAddToDeviceList) {
        numberOfDevices++;
        String[] tempList = new String[numberOfDevices];
        arrayCopy(deviceList, tempList);
        tempList[numberOfDevices - 1] = localName;
        deviceList = tempList;
        return true;
      }
    }
    return false;
  }

};
///////////////////////////////////////////////////////////////////////////////
//
// This class configures and manages the connection to the Serial port for
// the Arduino.
//
// Created: Chip Audette, Oct 2013
// Modified: through April 2014
// Modified again: Conor Russomanno Sept-Oct 2014
// Modified for Daisy (16-chan) OpenBCI V3: Conor Russomanno Nov 2014
// Modified Daisy Behaviors: Chip Audette Dec 2014
// Modified For Wifi Addition: AJ Keller July 2017
//
// Note: this class now expects the data format produced by OpenBCI V3.
//
/////////////////////////////////////////////////////////////////////////////

 //for logging raw bytes to an output file

//------------------------------------------------------------------------
//                       Global Variables & Instances
//------------------------------------------------------------------------


//these variables are used for "Kill Spikes" ... duplicating the last received data packet if packets were droppeds
boolean werePacketsDroppedSerial = false;
int numPacketsDroppedSerial = 0;


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
  if (iSerial.isOpenBCISerial(port)) {

    // boolean echoBytes = !cyton.isStateNormal();
    boolean echoBytes;

    if (iSerial.isStateNormal() != true) {  // || printingRegisters == true){
      echoBytes = true;
    } else {
      echoBytes = false;
    }
    iSerial.read(echoBytes);
    openBCI_byteCount++;
    if (iSerial.get_isNewDataPacketAvailable()) {
      println("woo got a new packet");
      //copy packet into buffer of data packets
      curDataPacketInd = (curDataPacketInd+1) % dataPacketBuff.length; //this is also used to let the rest of the code that it may be time to do something

      cyton.copyDataPacketTo(dataPacketBuff[curDataPacketInd]);
      iSerial.set_isNewDataPacketAvailable(false); //resets isNewDataPacketAvailable to false

      // KILL SPIKES!!!
      if(werePacketsDroppedSerial){
        for(int i = numPacketsDroppedSerial; i > 0; i--){
          int tempDataPacketInd = curDataPacketInd - i; //
          if(tempDataPacketInd >= 0 && tempDataPacketInd < dataPacketBuff.length){
            cyton.copyDataPacketTo(dataPacketBuff[tempDataPacketInd]);
          } else {
            cyton.copyDataPacketTo(dataPacketBuff[tempDataPacketInd+255]);
          }
          //put the last stored packet in # of packets dropped after that packet
        }

        //reset werePacketsDroppedSerial & numPacketsDroppedSerial
        werePacketsDroppedSerial = false;
        numPacketsDroppedSerial = 0;
      }

      switch (outputDataSource) {
      case OUTPUT_SOURCE_ODF:
        fileoutput_odf.writeRawData_dataPacket(dataPacketBuff[curDataPacketInd], cyton.get_scale_fac_uVolts_per_count(), cyton.get_scale_fac_accel_G_per_count(), PApplet.parseByte(0xC0));
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
      print(inByte);
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

//------------------------------------------------------------------------
//                       Classes
//------------------------------------------------------------------------

class InterfaceSerial {

  //here is the serial port for this OpenBCI board
  private Serial serial_openBCI = null;
  private boolean portIsOpen = false;

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

  int prefered_datamode = DATAMODE_BIN_WAUX;

  private int state = STATE_NOCOM;
  int dataMode = -1;
  int prevState_millis = 0;

  private int nEEGValuesPerPacket = 8; //defined by the data format sent by cyton boards
  private int nAuxValuesPerPacket = 3; //defined by the data format sent by cyton boards
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
  private boolean readyToSend = false; //system waits for $$$ after requesting information from OpenBCI board
  private long timeOfLastCommand = 0; //used when sync'ing to hardware

  //wait for $$$ to iterate... applies to commands expecting a response
  public boolean isReadyToSend() {
    return readyToSend;
  }
  public void setReadyToSend(boolean _readyToSend) {
    readyToSend = _readyToSend;
  }
  public int get_state() {
    return state;
  };
  public boolean get_isNewDataPacketAvailable() {
    return isNewDataPacketAvailable;
  }
  public void set_isNewDataPacketAvailable(boolean _isNewDataPacketAvailable) {
    isNewDataPacketAvailable = _isNewDataPacketAvailable;
  }

  //constructors
  InterfaceSerial() {
  };  //only use this if you simply want access to some of the constants
  InterfaceSerial(PApplet applet, String comPort, int baud, int nEEGValuesPerOpenBCI, boolean useAux, int nAuxValuesPerOpenBCI) {
    //choose data mode
    println("InterfaceSerial: prefered_datamode = " + prefered_datamode + ", nValuesPerPacket = " + nEEGValuesPerPacket);
    if (prefered_datamode == DATAMODE_BIN_WAUX) {
      if (!useAux) {
        //must be requesting the aux data, so change the referred data mode
        prefered_datamode = DATAMODE_BIN;
        nAuxValues = 0;
        //println("InterfaceSerial: nAuxValuesPerPacket = " + nAuxValuesPerPacket + " so setting prefered_datamode to " + prefered_datamode);
      }
    }

    dataMode = prefered_datamode;

    initDataPackets(nEEGValuesPerOpenBCI, nAuxValuesPerOpenBCI);

  }

  public void initDataPackets(int numEEG, int numAux) {
    nEEGValuesPerPacket = numEEG;
    nAuxValuesPerPacket = numAux;
    //allocate space for data packet
    rawReceivedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    missedDataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);  //this should always be 8 channels
    dataPacket = new DataPacket_ADS1299(nEEGValuesPerPacket, nAuxValuesPerPacket);            //this could be 8 or 16 channels

    for (int i = 0; i < nEEGValuesPerPacket; i++) {
      rawReceivedDataPacket.values[i] = 0;
      //prevDataPacket.values[i] = 0;
    }
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
  }

  // //manage the serial port
  public int openSerialPort(PApplet applet, String comPort, int baud) {

    output("Attempting to open Serial/COM port: " + openBCI_portName);
    try {
      println("InterfaceSerial: openSerialPort: attempting to open serial port: " + openBCI_portName);
      serial_openBCI = new Serial(applet, comPort, baud); //open the com port
      serial_openBCI.clear(); // clear anything in the com port's buffer
      portIsOpen = true;
      println("InterfaceSerial: openSerialPort: port is open (t)? ... " + portIsOpen);
      changeState(STATE_COMINIT);
      return 0;
    }
    catch (RuntimeException e) {
      if (e.getMessage().contains("<init>")) {
        serial_openBCI = null;
        System.out.println("InterfaceSerial: openSerialPort: port in use, trying again later...");
        portIsOpen = false;
      } else {
        println("RunttimeException: " + e);
        output("Error connecting to selected Serial/COM port. Make sure your board is powered up and your dongle is plugged in.");
        abandonInit = true; //global variable in OpenBCI_GUI.pde
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
    //   // println("InterfaceSerial: finalizeCOMINIT: Initializing Serial: millis() = " + millis());
    //   if ((millis() - prevState_millis) > COM_INIT_MSEC) {
    //     //serial_openBCI.write(command_activates + "\n"); println("Processing: Serial: activating filters");
    //     println("InterfaceSerial: finalizeCOMINIT: State = NORMAL");
    changeState(STATE_NORMAL);
    //     // startRunning();
    //   }
    // }
    return 0;
  }

  public int closeSDandSerialPort() {
    int returnVal=0;

    cyton.closeSDFile();

    readyToSend = false;
    returnVal = closeSerialPort();
    prevState_millis = 0;  //reset Serial state clock to use as a conditional for timing at the beginnign of systemUpdate()
    cyton.hardwareSyncStep = 0; //reset Hardware Sync step to be ready to go again...

    return returnVal;
  }

  public int closeSerialPort() {
    // if (serial_openBCI != null) {
    portIsOpen = false;
    if (serial_openBCI != null) {
      serial_openBCI.stop();
    }
    serial_openBCI = null;
    state = STATE_NOCOM;
    println("InterfaceSerial: closeSerialPort: closed");
    return 0;
  }

  public void updateSyncState(int sdSetting) {
    //has it been 3000 milliseconds since we initiated the serial port? We want to make sure we wait for the OpenBCI board to finish its setup()
    // println("0");

    if ( (millis() - prevState_millis > COM_INIT_MSEC) && (prevState_millis != 0) && (state == STATE_COMINIT) ) {
      state = STATE_SYNCWITHHARDWARE;
      timeOfLastCommand = millis();
      serial_openBCI.clear();
      cyton.potentialFailureMessage = "";
      cyton.defaultChannelSettings = ""; //clear channel setting string to be reset upon a new Init System
      cyton.daisyOrNot = ""; //clear daisyOrNot string to be reset upon a new Init System
      println("InterfaceSerial: systemUpdate: [0] Sending 'v' to OpenBCI to reset hardware in case of 32bit board...");
      serial_openBCI.write('v');
    }

    //if we are in SYNC WITH HARDWARE state ... trigger a command
    if ( (state == STATE_SYNCWITHHARDWARE) && (currentlySyncing == false) ) {
      if (millis() - timeOfLastCommand > 200 && readyToSend == true) {
        println("sdSetting: " + sdSetting);
        timeOfLastCommand = millis();
        cyton.hardwareSyncStep++;
        cyton.syncWithHardware(sdSetting);
      }
    }
  }

  public void sendChar(char val) {
    if (isSerialPortOpen()) {
      println("sending out: " + val);
      serial_openBCI.write(val);//send the value as ascii (with a newline character?)
    } else {
      println("nope no out: " + val);

    }
  }

  public void write(String msg) {
    if (isSerialPortOpen()) {
      serial_openBCI.write(msg);
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

  public void clear() {
    if (serial_openBCI != null) {
      serial_openBCI.clear();
    }
  }

  //read from the serial port
  public int read() {
    return read(false);
  }
  public int read(boolean echoChar) {
    //println("InterfaceSerial: read(): State: " + state);
    //get the byte
    byte inByte;
    if (isSerialPortOpen()) {
      inByte = PApplet.parseByte(serial_openBCI.read());
    } else {
      println("InterfaceSerial port not open aborting.");
      return 0;
    }
    print(inByte);
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

      if (cyton.hardwareSyncStep == 0 && inASCII != '$') {
        cyton.potentialFailureMessage+=inASCII;
      }

      if (cyton.hardwareSyncStep == 1 && inASCII != '$') {
        cyton.daisyOrNot+=inASCII;
        //if hardware returns 8 because daisy is not attached, switch the GUI mode back to 8 channels
        // if(nchan == 16 && char(daisyOrNot.substring(daisyOrNot.length() - 1)) == '8'){
        if (nchan == 16 && cyton.daisyOrNot.charAt(cyton.daisyOrNot.length() - 1) == '8') {
          // verbosePrint(" received from OpenBCI... Switching to nchan = 8 bc daisy is not present...");
          verbosePrint(" received from OpenBCI... Abandoning hardware initiation.");
          abandonInit = true;
          // haltSystem();

          // updateToNChan(8);
          //
          // //initialize the FFT objects
          // for (int Ichan=0; Ichan < nchan; Ichan++) {
          //   verbosePrint("Init FFT Buff \u2013 "+Ichan);
          //   fftBuff[Ichan] = new FFT(Nfft, getSampleRateSafe());
          // }  //make the FFT objects
          //
          // initializeFFTObjects(fftBuff, dataBuffY_uV, Nfft, getSampleRateSafe());
          // setupWidgetManager();
        }
      }

      if (cyton.hardwareSyncStep == 3 && inASCII != '$') { //if we're retrieving channel settings from OpenBCI
        cyton.defaultChannelSettings+=inASCII;
      }

      //if the last three chars are $$$, it means we are moving on to the next stage of initialization
      if (prev3chars[0] == EOT[0] && prev3chars[1] == EOT[1] && prev3chars[2] == EOT[2]) {
        verbosePrint(" > EOT detected...");
        // Added for V2 system down rejection line
        if (cyton.hardwareSyncStep == 0) {
          // Failure: Communications timeout - Device failed to poll Host$$$
          if (cyton.potentialFailureMessage.equals(failureMessage)) {
            closeLogFile();
            return 0;
          }
        }
        // hardwareSyncStep++;
        prev3chars[2] = '#';
        if (cyton.hardwareSyncStep == 3) {
          println("InterfaceSerial: read(): x");
          println(cyton.defaultChannelSettings);
          println("InterfaceSerial: read(): y");
          // gui.cc.loadDefaultChannelSettings();
          w_timeSeries.hsc.loadDefaultChannelSettings();
          println("InterfaceSerial: read(): z");
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
        System.err.println("InterfaceSerial: read(): Caught IOException: " + e.getMessage());
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

    //println("InterfaceSerial: interpretBinaryStream: PACKET_readstate " + PACKET_readstate);
    switch (PACKET_readstate) {
    case 0:
      //look for header byte
      if (actbyte == PApplet.parseByte(0xA0)) {          // look for start indicator
        // println("InterfaceSerial: interpretBinaryStream: found 0xA0");
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
          werePacketsDroppedSerial = true; //set this true to activate packet duplication in serialEvent

          if(rawReceivedDataPacket.sampleIndex < prevSampleIndex){   //handle the situation in which the index jumps from 250s past 255, and back to 0
            numPacketsDroppedSerial = (rawReceivedDataPacket.sampleIndex+255) - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
          } else {
            numPacketsDroppedSerial = rawReceivedDataPacket.sampleIndex - prevSampleIndex; //calculate how many times the last received packet should be duplicated...
          }

          println("InterfaceSerial: apparent sampleIndex jump from Serial data: " + prevSampleIndex + " to  " + rawReceivedDataPacket.sampleIndex + ".  Keeping packet. (" + serialErrorCounter + ")");
          if (outputDataSource == OUTPUT_SOURCE_BDF) {
            int fakePacketsToWrite = (rawReceivedDataPacket.sampleIndex - prevSampleIndex) - 1;
            for (int i = 0; i < fakePacketsToWrite; i++) {
              fileoutput_bdf.writeRawData_dataPacket(missedDataPacket);
            }
            println("InterfaceSerial: because BDF, wrote " + fakePacketsToWrite + " empty data packet(s)");
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
          // println("InterfaceSerial: interpretBinaryStream: localChannelCounter = " + localChannelCounter);
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
          // println("InterfaceSerial: interpretBinaryStream: Accel Data: " + rawReceivedDataPacket.auxValues[0] + ", " + rawReceivedDataPacket.auxValues[1] + ", " + rawReceivedDataPacket.auxValues[2]);
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
        // println("InterfaceSerial: interpretBinaryStream: found end byte. Setting isNewDataPacketAvailable to TRUE");
        isNewDataPacketAvailable = true; //original place for this.  but why not put it in the previous case block
        flag_copyRawDataToFullData = true;  //time to copy the raw data packet into the full data packet (mainly relevant for 16-chan OpenBCI)
      } else {
        serialErrorCounter++;
        println("InterfaceSerial: interpretBinaryStream: Actbyte = " + actbyte);
        println("InterfaceSerial: interpretBinaryStream: expecteding end-of-packet byte is missing.  Discarding packet. (" + serialErrorCounter + ")");
      }
      PACKET_readstate=0;  // either way, look for next packet
      break;
    default:
      println("InterfaceSerial: interpretBinaryStream: Unknown byte: " + actbyte + " .  Continuing...");
      PACKET_readstate=0;  // look for next packet
    }

    if (flag_copyRawDataToFullData) {
      copyRawDataToFullData();
    }
  } // end of interpretBinaryStream



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

};
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
  PImage presentationSlides[] = new PImage[slideCount];
  float timeOfLastSlideChange = 0;
  int currentSlide = 0;
  boolean lockSlides = false;

  Presentation (){
    //loading presentation images
    println("attempting to load images for presentation...");
    presentationSlides[0] = loadImage("prez-images/Presentation.000.jpg");
    presentationSlides[1] = loadImage("prez-images/Presentation.001.jpg");
    presentationSlides[2] = loadImage("prez-images/Presentation.002.jpg");
    presentationSlides[3] = loadImage("prez-images/Presentation.003.jpg");
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

    image(presentationSlides[currentSlide], 0, 0, width, height);


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
//  Radios_Config will be used for radio configuration
//  integration. Also handles functions such as the "autconnect"
//  feature.
//
//  Created: Colin Fausnaught, July 2016
//
//  Handles interactions between the radio system and OpenBCI systems.
//  It is important to note that this is using Serial communication directly
//  rather than the Cyton class. I just found this easier to work
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
          print("blasss try "); print(i); print(" "); print(serialPort); println(" at 115200 baud");
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
      serial_output = new Serial(this, openBCI_portName, openBCI_baud); //open the com port
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
  println("Radios_Config: system_status");

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
  println("Radios_Config: scan_channels");
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
  println("Radios_Config: get_channel");
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
  println("Radios_Config: set_channel");
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
  println("Radios_Config: set_ovr_channel");
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

  Button fpsButton;
  Button highRezButton;

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

    fpsButton = new Button(3+3+256, 3, 73, 26, "XX" + " fps", fontInfo.buttonLabel_size);
    if(frameRateCounter==0){
      fpsButton.setString("24 fps");
    }
    if(frameRateCounter==1){
      fpsButton.setString("30 fps");
    }
    if(frameRateCounter==2){
      fpsButton.setString("45 fps");
    }
    if(frameRateCounter==3){
      fpsButton.setString("60 fps");
    }

    fpsButton.setFont(h3, 16);
    fpsButton.setHelpText("If you're having latency issues, try adjusting the frame rate and see if it helps!");

    highRezButton = new Button(3+3+256+73+3, 3, 26, 26, "XX", fontInfo.buttonLabel_size);
    controlPanelCollapser.setFont(h3, 16);

    //top right buttons from right to left
    //int butNum = 1;
    //tutorialsButton = new Button(width - 3*(butNum) - 80, 3, 80, 26, "Help", fontInfo.buttonLabel_size);
    //tutorialsButton.setFont(h3, 16);
    //tutorialsButton.setHelpText("Click to find links to helpful online tutorials and getting started guides. Also, check out how to create custom widgets for the GUI!");

    //butNum = 2;
    //issuesButton = new Button(width - 3*(butNum) - 80 - tutorialsButton.but_dx, 3, 80, 26, "Issues", fontInfo.buttonLabel_size);
    //issuesButton.setHelpText("If you have suggestions or want to share a bug you've found, please create an issue on the GUI's Github repo!");
    //issuesButton.setURL("https://github.com/OpenBCI/OpenBCI_GUI/issues");
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
    filtNotchButton.setHelpText("Here you can adjust the Notch Filter that is applied to all \"Filtered\" data.");

    filtBPButton = new Button(11 + stopButton.but_dx + 70, 35, 70, 26, "BP Filt\n" + dataProcessing.getShortFilterDescription(), fontInfo.buttonLabel_size);
    filtBPButton.setFont(p5, 12);
    filtBPButton.setHelpText("Here you can adjust the Band Pass Filter that is applied to all \"Filtered\" data.");

    //right to left in top right (secondary nav)
    layoutButton = new Button(width - 3 - 60, 35, 60, 26, "Layout", fontInfo.buttonLabel_size);
    layoutButton.setHelpText("Here you can alter the overall layout of the GUI, allowing for different container configurations with more or less widgets.");
    layoutButton.setFont(h4, 14);

    updateSecondaryNavButtonsColor();
  }

  public void updateNavButtonsBasedOnColorScheme(){
    if(colorScheme == COLOR_SCHEME_DEFAULT){
      controlPanelCollapser.setColorNotPressed(color(255));
      fpsButton.setColorNotPressed(color(255));
      highRezButton.setColorNotPressed(color(255));
      //issuesButton.setColorNotPressed(color(255));
      //shopButton.setColorNotPressed(color(255));
      //tutorialsButton.setColorNotPressed(color(255));

      controlPanelCollapser.textColorNotActive = color(bgColor);
      fpsButton.textColorNotActive = color(bgColor);
      highRezButton.textColorNotActive = color(bgColor);
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
      fpsButton.setColorNotPressed(openbciBlue);
      highRezButton.setColorNotPressed(openbciBlue);
      //issuesButton.setColorNotPressed(openbciBlue);
      //shopButton.setColorNotPressed(openbciBlue);
      //tutorialsButton.setColorNotPressed(openbciBlue);

      controlPanelCollapser.textColorNotActive = color(255);
      fpsButton.textColorNotActive = color(255);
      highRezButton.textColorNotActive = color(255);
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
      image(logo_blue, width/2 - (128/2) - 2, 6, 128, 22);
    } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
      noStroke();
      fill(100);
      fill(57,128,204);
      rect(0, 0, width, topNav_h);
      stroke(bgColor);
      fill(31,69,110);
      rect(-1, 0, width+2, navBarHeight);
      image(logo_white, width/2 - (128/2) - 2, 6, 188, 25);
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
    fpsButton.draw();
    // highRezButton.draw();
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
      tutorialSelector.screenResized();
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

    if(fpsButton.isMouseHere()){
      fpsButton.setIsActive(true);
    }

    // Conor's attempt at adjusting the GUI to be 2x in size for High DPI screens ... attempt failed
    // if(highRezButton.isMouseHere()){
    //   highRezButton.setIsActive(true);
    // }

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

    if (fpsButton.isMouseHere() && fpsButton.isActive()) {
      toggleFrameRate();
    }

    // Conor's attempt at adjusting the GUI to be 2x in size for High DPI screens ... attempt failed
    // if (highRezButton.isMouseHere() && highRezButton.isActive()) {
    //   toggleHighDPI();
    // }

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

    fpsButton.setIsActive(false);
    highRezButton.setIsActive(false);
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
    tempTutorialButton.setURL("http://docs.openbci.com/Tutorials/01-Cyton_Getting%20Started_Guide");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 1;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "Testing Impedance");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://docs.openbci.com/Tutorials/01-Cyton_Getting%20Started_Guide#cyton-getting-started-guide-v-connect-yourself-to-openbci-4-launch-the-gui-and-adjust-your-channel-settings");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 2;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "OpenBCI Forum");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://openbci.com/index.php/forum/");
    tutorialOptions.add(tempTutorialButton);

    buttonNumber = 3;
    h = margin*(buttonNumber+2) + b_h*(buttonNumber+1);
    tempTutorialButton = new Button(x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h, "Building Custom Widgets");
    tempTutorialButton.setFont(p5, 12);
    tempTutorialButton.setURL("http://docs.openbci.com/Tutorials/15-Custom_Widgets");
    tutorialOptions.add(tempTutorialButton);

  }

  public void updateLayoutOptionButtons(){

  }

}

////////////////////////////////////////////////////
//
//  W_AnalogRead is used to visiualze analog voltage values
//
//  Created: AJ Keller
//
//
///////////////////////////////////////////////////,

class W_AnalogRead extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  int numAnalogReadBars;
  float xF, yF, wF, hF;
  float ts_padding;
  float ts_x, ts_y, ts_h, ts_w; //values for actual time series chart (rectangle encompassing all analogReadBars)
  float plotBottomWell;
  float playbackWidgetHeight;
  int analogReadBarHeight;

  AnalogReadBar[] analogReadBars;

  int[] xLimOptions = {1, 3, 5, 7}; // number of seconds (x axis of graph)
  int[] yLimOptions = {0, 50, 100, 200, 400, 1000, 10000}; // 0 = Autoscale ... everything else is uV

  boolean allowSpillover = false;

  TextBox[] chanValuesMontage;
  boolean showMontageValues;

  private boolean visible = true;
  private boolean updating = true;

  int startingVertScaleIndex = 5;
  int startingHoriztonalScaleIndex = 2;

  private boolean hasScrollbar = false;

  Button analogModeButton;

  W_AnalogRead(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function

    addDropdown("VertScale_AR", "Vert Scale", Arrays.asList("Auto", "50", "100", "200", "400", "1000", "10000"), startingVertScaleIndex);
    addDropdown("Duration_AR", "Window", Arrays.asList("1 sec", "3 sec", "5 sec", "7 sec"), startingHoriztonalScaleIndex);
    // addDropdown("Spillover", "Spillover", Arrays.asList("False", "True"), 0);

    //set number of anaolg reads
    if (cyton.isWifi()) {
      numAnalogReadBars = 2;
    } else {
      numAnalogReadBars = 3;
    }

    xF = PApplet.parseFloat(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
    yF = PApplet.parseFloat(y);
    wF = PApplet.parseFloat(w);
    hF = PApplet.parseFloat(h);

    plotBottomWell = 45.0f; //this appears to be an arbitrary vertical space adds GPlot leaves at bottom, I derived it through trial and error
    ts_padding = 10.0f;
    ts_x = xF + ts_padding;
    ts_y = yF + (ts_padding);
    ts_w = wF - ts_padding*2;
    ts_h = hF - playbackWidgetHeight - plotBottomWell - (ts_padding*2);
    analogReadBarHeight = PApplet.parseInt(ts_h/numAnalogReadBars);

    analogReadBars = new AnalogReadBar[numAnalogReadBars];

    //create our channel bars and populate our analogReadBars array!
    for(int i = 0; i < numAnalogReadBars; i++){
      println("init analog read bar " + i);
      int analogReadBarY = PApplet.parseInt(ts_y) + i*(analogReadBarHeight); //iterate through bar locations
      AnalogReadBar tempBar = new AnalogReadBar(_parent, i+5, PApplet.parseInt(ts_x), analogReadBarY, PApplet.parseInt(ts_w), analogReadBarHeight); //int _channelNumber, int _x, int _y, int _w, int _h
      analogReadBars[i] = tempBar;
      analogReadBars[i].adjustVertScale(yLimOptions[startingVertScaleIndex]);
      analogReadBars[i].adjustTimeAxis(xLimOptions[startingHoriztonalScaleIndex]);
    }

    analogModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Analog Read On", 12);
    analogModeButton.setCornerRoundess((int)(navHeight-6));
    analogModeButton.setFont(p6,10);
    // analogModeButton.setStrokeColor((int)(color(150)));
    // analogModeButton.setColorNotPressed(openbciBlue);
    analogModeButton.setColorNotPressed(color(57,128,204));
    analogModeButton.textColorNotActive = color(255);
    // analogModeButton.setStrokeColor((int)(color(138, 182, 229, 100)));
    analogModeButton.hasStroke(false);
    // analogModeButton.setColorNotPressed((int)(color(138, 182, 229)));
    if (cyton.isWifi()) {
      analogModeButton.setHelpText("Click this button to activate/deactivate the analog read of your Cyton board from A5(D11) and A6(D12)");
    } else {
      analogModeButton.setHelpText("Click this button to activate/deactivate the analog read of your Cyton board from A5(D11), A6(D12) and A7(D13)");
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

  public void update(){
    if(visible && updating){
      super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

      //put your code here...
      //update channel bars ... this means feeding new EEG data into plots
      for(int i = 0; i < numAnalogReadBars; i++){
        analogReadBars[i].update();
      }
    }
  }

  public void draw(){
    if(visible){
      super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

      //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
      pushStyle();
      //draw channel bars
      analogModeButton.draw();
      if (cyton.getBoardMode() != BOARD_MODE_ANALOG) {
        analogModeButton.setString("Turn Analog Read On");
      } else {
        analogModeButton.setString("Turn Analog Read Off");
        for(int i = 0; i < numAnalogReadBars; i++){
          analogReadBars[i].draw();
        }
      }
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
    analogReadBarHeight = PApplet.parseInt(ts_h/numAnalogReadBars);

    for(int i = 0; i < numAnalogReadBars; i++){
      int analogReadBarY = PApplet.parseInt(ts_y) + i*(analogReadBarHeight); //iterate through bar locations
      analogReadBars[i].screenResized(PApplet.parseInt(ts_x), analogReadBarY, PApplet.parseInt(ts_w), analogReadBarHeight); //bar x, bar y, bar w, bar h
    }

    analogModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (analogModeButton.isMouseHere()) {
      analogModeButton.setIsActive(true);
    }
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(analogModeButton.isActive && analogModeButton.isMouseHere()){
      // println("analogModeButton...");
      if(cyton.isPortOpen()) {
        if (cyton.getBoardMode() != BOARD_MODE_ANALOG) {
          cyton.setBoardMode(BOARD_MODE_ANALOG);
          if (cyton.isWifi()) {
            output("Starting to read analog inputs on pin marked A5 (D11) and A6 (D12)");
          } else {
            output("Starting to read analog inputs on pin marked A5 (D11), A6 (D12) and A7 (D13)");
          }
        } else {
          cyton.setBoardMode(BOARD_MODE_DEFAULT);
          output("Starting to read accelerometer");
        }
      }
    }
    analogModeButton.setIsActive(false);
  }
};

//These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void VertScale_AR(int n) {
  if (n==0) { //autoscale
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(0);
    }
  } else if(n==1) { //50uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(50);
    }
  } else if(n==2) { //100uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(100);
    }
  } else if(n==3) { //200uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(200);
    }
  } else if(n==4) { //400uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(400);
    }
  } else if(n==5) { //1000uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(1000);
    }
  } else if(n==6) { //10000uV
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustVertScale(10000);
    }
  }
  closeAllDropdowns();
}

//triggered when there is an event in the LogLin Dropdown
public void Duration_AR(int n) {
  // println("adjust duration to: ");
  if(n==0){ //set time series x axis to 1 secconds
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustTimeAxis(1);
    }
  } else if(n==1){ //set time series x axis to 3 secconds
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustTimeAxis(3);
    }
  } else if(n==2){ //set to 5 seconds
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustTimeAxis(5);
    }
  } else if(n==3){ //set to 7 seconds (max due to arry size ... 2000 total packets saved)
    for(int i = 0; i < w_analogRead.numAnalogReadBars; i++){
      w_analogRead.analogReadBars[i].adjustTimeAxis(7);
    }
  }
  closeAllDropdowns();
}

//========================================================================================================================
//                      Analog Voltage BAR CLASS -- Implemented by Analog Read Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
class AnalogReadBar{

  int analogInputPin;
  int auxValuesPosition;
  String analogInputString;
  int x, y, w, h;
  boolean isOn; //true means data is streaming and channel is active on hardware ... this will send message to OpenBCI Hardware

  GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
  GPointsArray analogReadPoints;
  int nPoints;
  int numSeconds;
  float timeBetweenPoints;

  int channelColor; //color of plot trace

  boolean isAutoscale; //when isAutoscale equals true, the y-axis of each channelBar will automatically update to scale to the largest visible amplitude
  int autoScaleYLim = 0;

  TextBox analogValue;
  TextBox analogPin;
  TextBox digitalPin;

  boolean drawAnalogValue;
  int lastProcessedDataPacketInd = 0;

  int[] analogReadData;

  AnalogReadBar(PApplet _parent, int _analogInputPin, int _x, int _y, int _w, int _h){ // channel number, x/y location, height, width

    analogInputPin = _analogInputPin;
    int digitalPinNum = 0;
    if (analogInputPin == 7) {
      auxValuesPosition = 2;
      digitalPinNum = 13;
    } else if (analogInputPin == 6) {
      auxValuesPosition = 1;
      digitalPinNum = 12;
    } else {
      analogInputPin = 5;
      auxValuesPosition = 0;
      digitalPinNum = 11;
    }

    analogInputString = str(analogInputPin);
    isOn = true;

    x = _x;
    y = _y;
    w = _w;
    h = _h;

    numSeconds = 5;
    plot = new GPlot(_parent);
    plot.setPos(x + 36 + 4, y);
    plot.setDim(w - 36 - 4, h);
    plot.setMar(0f, 0f, 0f, 0f);
    plot.setLineColor((int)channelColors[(auxValuesPosition)%8]);
    plot.setXLim(-3.2f,-2.9f);
    plot.setYLim(-200,200);
    plot.setPointSize(2);
    plot.setPointColor(0);
    if (cyton.isWifi()) {
      if(auxValuesPosition == 1){
        plot.getXAxis().setAxisLabelText("Time (s)");
      }
    } else {
      if(auxValuesPosition == 2){
        plot.getXAxis().setAxisLabelText("Time (s)");
      }
    }

    nPoints = nPointsBasedOnDataSource();

    analogReadData = new int[nPoints];

    analogReadPoints = new GPointsArray(nPoints);
    timeBetweenPoints = (float)numSeconds / (float)nPoints;

    for (int i = 0; i < nPoints; i++) {
      float time = -(float)numSeconds + (float)i*timeBetweenPoints;
      float analog_value = 0.0f; //0.0 for all points to start
      GPoint tempPoint = new GPoint(time, analog_value);
      analogReadPoints.set(i, tempPoint);
    }

    plot.setPoints(analogReadPoints); //set the plot with 0.0 for all analogReadPoints to start

    analogValue = new TextBox("t", x + 36 + 4 + (w - 36 - 4) - 2, y + h);
    analogValue.textColor = color(bgColor);
    analogValue.alignH = RIGHT;
    // analogValue.alignV = TOP;
    analogValue.drawBackground = true;
    analogValue.backgroundColor = color(255,255,255,125);

    analogPin = new TextBox("A" + analogInputString, x+3, y + h);
    analogPin.textColor = color(bgColor);
    analogPin.alignH = CENTER;
    digitalPin = new TextBox("(D" + digitalPinNum + ")", x+3, y + h + 12);
    digitalPin.textColor = color(bgColor);
    digitalPin.alignH = CENTER;

    drawAnalogValue = true;

  }

  public void update(){

    //update the voltage value text string
    String fmt; float val;

    //update the voltage values
    val = hub.validAccelValues[auxValuesPosition];
    analogValue.string = String.format(getFmt(val),val);

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
    int numSamplesToProcess = curDataPacketInd - lastProcessedDataPacketInd;
    if (numSamplesToProcess < 0) {
      numSamplesToProcess += dataPacketBuff.length;
    }

    // Shift internal ring buffer numSamplesToProcess
    if (numSamplesToProcess > 0) {
      for(int i = 0; i < analogReadData.length - numSamplesToProcess; i++){
        analogReadData[i] = analogReadData[i + numSamplesToProcess];
      }
    }

    // for each new sample
    int samplesProcessed = 0;
    while (samplesProcessed < numSamplesToProcess) {
      lastProcessedDataPacketInd++;

      // Watch for wrap around
      if (lastProcessedDataPacketInd > dataPacketBuff.length - 1) {
        lastProcessedDataPacketInd = 0;
      }

      int voltage = dataPacketBuff[lastProcessedDataPacketInd].auxValues[auxValuesPosition];

      analogReadData[analogReadData.length - numSamplesToProcess + samplesProcessed] = voltage;

      samplesProcessed++;
    }

    if (numSamplesToProcess > 0) {
      for (int i = 0; i < nPoints; i++) {
        float timey = -(float)numSeconds + (float)i*timeBetweenPoints;
        float voltage = analogReadData[i];

        GPoint tempPoint = new GPoint(timey, voltage);
        analogReadPoints.set(i, tempPoint);

      }
      plot.setPoints(analogReadPoints); //reset the plot with updated analogReadPoints
    }
  }

  public void draw(){
    pushStyle();

    //draw plot
    stroke(31,69,110, 50);
    fill(color(125,30,12,30));

    rect(x + 36 + 4, y, w - 36 - 4, h);

    plot.beginDraw();
    plot.drawBox(); // we won't draw this eventually ...
    plot.drawGridLines(0);
    plot.drawLines();
    // plot.drawPoints();
    // plot.drawYAxis();
    if (cyton.isWifi()) {
      if(auxValuesPosition == 1){ //only draw the x axis label on the bottom channel bar
        plot.drawXAxis();
        plot.getXAxis().draw();
      }
    } else {
      if(auxValuesPosition == 2){ //only draw the x axis label on the bottom channel bar
        plot.drawXAxis();
        plot.getXAxis().draw();
      }
    }

    plot.endDraw();

    if(drawAnalogValue){
      analogValue.draw();
      analogPin.draw();
      digitalPin.draw();
    }

    popStyle();
  }

  public int nPointsBasedOnDataSource(){
    return numSeconds * (int)getSampleRateSafe();
  }

  public void adjustTimeAxis(int _newTimeSize){
    numSeconds = _newTimeSize;
    plot.setXLim(-_newTimeSize,0);

    nPoints = nPointsBasedOnDataSource();

    analogReadPoints = new GPointsArray(nPoints);
    if(_newTimeSize > 1){
      plot.getXAxis().setNTicks(_newTimeSize);  //sets the number of axis divisions...
    }else{
      plot.getXAxis().setNTicks(10);
    }
    if (w_analogRead != null) {
      if(w_analogRead.isUpdating()){
        updatePlotPoints();
      }
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
      if(PApplet.parseInt(abs(analogReadPoints.getY(i))) > autoScaleYLim){
        autoScaleYLim = PApplet.parseInt(abs(analogReadPoints.getY(i)));
      }
    }
    plot.setYLim(-autoScaleYLim, autoScaleYLim);
  }

  public void screenResized(int _x, int _y, int _w, int _h){
    x = _x;
    y = _y;
    w = _w;
    h = _h;

    //reposition & resize the plot
    plot.setPos(x + 36 + 4, y);
    plot.setDim(w - 36 - 4, h);

    analogValue.x = x + 36 + 4 + (w - 36 - 4) - 2;
    analogValue.y = y + h;

    analogPin.x = x + 14;
    analogPin.y = y + PApplet.parseInt(h/2.0f);
    digitalPin.x = analogPin.x;
    digitalPin.y = analogPin.y + 12;
  }
};

////////////////////////////////////////////////////
//
//    W_BandPowers.pde
//
//    This is a band power visualization widget!
//    (Couldn't think up more)
//    This is for visualizing the power of each brainwave band: delta, theta, alpha, beta, gamma
//    Averaged over all channels
//
//    Created by: Wangshu Sun, May 2017
//
///////////////////////////////////////////////////,

class W_BandPower extends Widget {

  GPlot plot3;
  String bands[] = {"DELTA", "THETA", "ALPHA", "BETA", "GAMMA"};

  W_BandPower(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    // addDropdown("Dropdown1", "Drop 1", Arrays.asList("A", "B"), 0);
    // addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    // addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

    // Setup for the third plot
    plot3 = new GPlot(_parent, x, y-navHeight, w, h+navHeight);
    plot3.setPos(x, y);
    plot3.setDim(w, h);
    plot3.setLogScale("y");
    plot3.setYLim(0.1f, 100);
    plot3.setXLim(0, 5);
    plot3.getYAxis().setNTicks(9);
    plot3.getTitle().setTextAlignment(LEFT);
    plot3.getTitle().setRelativePos(0);
    plot3.getYAxis().getAxisLabel().setText("(uV)^2 / Hz per channel");
    plot3.getYAxis().getAxisLabel().setTextAlignment(RIGHT);
    plot3.getYAxis().getAxisLabel().setRelativePos(1);
    // plot3.setPoints(points3);
    plot3.startHistograms(GPlot.VERTICAL);
    plot3.getHistogram().setDrawLabels(true);
    //plot3.getHistogram().setRotateLabels(true);
    plot3.getHistogram().setBgColors(new int[] {
      color(0, 0, 255, 50), color(0, 0, 255, 100),
      color(0, 0, 255, 150), color(0, 0, 255, 200)
    }
    );

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    GPointsArray points3 = new GPointsArray(dataProcessing.headWidePower.length);
    points3.add(DELTA + 0.5f, dataProcessing.headWidePower[DELTA], "DELTA");
    points3.add(THETA + 0.5f, dataProcessing.headWidePower[THETA], "THETA");
    points3.add(ALPHA + 0.5f, dataProcessing.headWidePower[ALPHA], "ALPHA");
    points3.add(BETA + 0.5f, dataProcessing.headWidePower[BETA], "BETA");
    points3.add(GAMMA + 0.5f, dataProcessing.headWidePower[GAMMA], "GAMMA");

    plot3.setPoints(points3);
    plot3.getTitle().setText("Band Power");
    

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    // Draw the third plot
    plot3.beginDraw();
    plot3.drawBackground();
    plot3.drawBox();
    plot3.drawYAxis();
    plot3.drawTitle();
    plot3.drawHistograms();
    plot3.endDraw();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    plot3.setPos(x, y-navHeight);//update position
    plot3.setOuterDim(w, h+navHeight);//update dimensions


  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  //add custom functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget

  }

};

// //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
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
//
///////////////////////////////////////////////////,

class W_darwin extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...
  Button widgetTemplateButton;

  W_darwin(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    //addDropdown("Dropdown1", "Drop 1", Arrays.asList("A", "B"), 0);
    //addDropdown("Dropdown2", "Drop 2", Arrays.asList("C", "D", "E"), 1);
    //addDropdown("Dropdown3", "Drop 3", Arrays.asList("F", "G", "H", "I"), 3);

    //widgetTemplateButton = new Button (x + w/2, y + h/2, 200, navHeight, "Design Your Own Widget!", 12);
    //widgetTemplateButton.setFont(p4, 14);
    //widgetTemplateButton.setURL("http://docs.openbci.com/OpenBCI%20Software/");

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();
    image(darwinLogo, x , y , w , h );
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
   // widgetTemplateButton.setPos(x + w/2 - widgetTemplateButton.but_dx/2, y + h/2 - widgetTemplateButton.but_dy/2);


  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    //if(widgetTemplateButton.isMouseHere()){
    //  widgetTemplateButton.setIsActive(true);
    //}

  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    //if(widgetTemplateButton.isActive && widgetTemplateButton.isMouseHere()){
    //  widgetTemplateButton.goToURL();
    //}
    //widgetTemplateButton.setIsActive(false);

  }

  //add custom functions here
  public void customFunction(){
    //this is a fake function... replace it with something relevant to this widget

  }

};

////////////////////////////////////////////////////
//
//  W_DigitalRead is used to visiualze digital input values
//
//  Created: AJ Keller
//
//
///////////////////////////////////////////////////,

class W_DigitalRead extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  int numDigitalReadDots;
  float xF, yF, wF, hF;
  int dot_padding;
  float dot_x, dot_y, dot_h, dot_w; //values for actual time series chart (rectangle encompassing all digitalReadDots)
  float plotBottomWell;
  float playbackWidgetHeight;
  int digitalReadDotHeight;

  DigitalReadDot[] digitalReadDots;

  TextBox[] chanValuesMontage;
  boolean showMontageValues;

  private boolean visible = true;
  private boolean updating = true;

  Button digitalModeButton;

  W_DigitalRead(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function

    //set number of digital reads
    if (cyton.isWifi()) {
      numDigitalReadDots = 3;
    } else {
      numDigitalReadDots = 5;
    }

    xF = PApplet.parseFloat(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
    yF = PApplet.parseFloat(y);
    wF = PApplet.parseFloat(w);
    hF = PApplet.parseFloat(h);

    dot_padding = 10;
    dot_x = xF + dot_padding;
    dot_y = yF + (dot_padding);
    dot_w = wF - dot_padding*2;
    dot_h = hF - playbackWidgetHeight - plotBottomWell - (dot_padding*2);
    digitalReadDotHeight = PApplet.parseInt(dot_h/numDigitalReadDots);

    digitalReadDots = new DigitalReadDot[numDigitalReadDots];

    //create our channel bars and populate our digitalReadDots array!
    for(int i = 0; i < numDigitalReadDots; i++){
      int digitalReadDotY = PApplet.parseInt(dot_y) + i*(digitalReadDotHeight); //iterate through bar locations
      int digitalReadDotX = PApplet.parseInt(dot_x) + i*(digitalReadDotHeight); //iterate through bar locations
      int digitalPin = 0;
      if (i == 0) {
        digitalPin = 11;
      } else if (i == 1) {
        digitalPin = 12;
      } else if (i == 2) {
        if (cyton.isWifi()) {
          digitalPin = 17;
        } else {
          digitalPin = 13;
        }
      } else if (i == 3) {
        digitalPin = 17;
      } else {
        digitalPin = 18;
      }
      DigitalReadDot tempDot = new DigitalReadDot(_parent, digitalPin, digitalReadDotX, digitalReadDotY, PApplet.parseInt(dot_w), digitalReadDotHeight, dot_padding);
      digitalReadDots[i] = tempDot;
    }

    digitalModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Analog Read On", 12);
    digitalModeButton.setCornerRoundess((int)(navHeight-6));
    digitalModeButton.setFont(p6,10);
    digitalModeButton.setColorNotPressed(color(57,128,204));
    digitalModeButton.textColorNotActive = color(255);
    digitalModeButton.hasStroke(false);

    if (cyton.isWifi()) {
      digitalModeButton.setHelpText("Click this button to activate/deactivate digital reading on the Cyton D11, D12, and D17");
    } else {
      digitalModeButton.setHelpText("Click this button to activate/deactivate digital reading on the Cyton D11, D12, D13, D17 and D18");
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

  public void update(){
    if(visible && updating){
      super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

      //put your code here...
      //update channel bars ... this means feeding new EEG data into plots
      for(int i = 0; i < numDigitalReadDots; i++){
        digitalReadDots[i].update();
      }
    }
  }

  public void draw(){
    if(visible){
      super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

      //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
      pushStyle();
      //draw channel bars
      digitalModeButton.draw();
      if (cyton.getBoardMode() != BOARD_MODE_DIGITAL) {
        digitalModeButton.setString("Turn Digital Read On");
      } else {
        digitalModeButton.setString("Turn Digital Read Off");
        for(int i = 0; i < numDigitalReadDots; i++){
          digitalReadDots[i].draw();
        }
      }
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
    // println("w_digitalRead: screenResized: x: " + x + " y: " + y + " w: "+ w + " h: " + h + " navBarHeight: " + navBarHeight);

    if (wF > hF) {
      digitalReadDotHeight = PApplet.parseInt(hF/(numDigitalReadDots+1));
    } else {
      digitalReadDotHeight = PApplet.parseInt(wF/(numDigitalReadDots+1));
    }

    if (numDigitalReadDots == 3) {
      digitalReadDots[0].screenResized(x+PApplet.parseInt(wF*(1.0f/3.0f)), y+PApplet.parseInt(hF*(1.0f/3.0f)), digitalReadDotHeight, digitalReadDotHeight); //bar x, bar y, bar w, bar h
      digitalReadDots[1].screenResized(x+PApplet.parseInt(wF/2), y+PApplet.parseInt(hF/2), digitalReadDotHeight, digitalReadDotHeight); //bar x, bar y, bar w, bar h
      digitalReadDots[2].screenResized(x+PApplet.parseInt(wF*(2.0f/3.0f)), y+PApplet.parseInt(hF*(2.0f/3.0f)), digitalReadDotHeight, digitalReadDotHeight); //bar x, bar y, bar w, bar h
    } else {
      int y_pad = y + dot_padding;
      digitalReadDots[0].screenResized(x+PApplet.parseInt(wF*(1.0f/8.0f)), y_pad+PApplet.parseInt(hF*(1.0f/8.0f)), digitalReadDotHeight, digitalReadDotHeight);
      digitalReadDots[2].screenResized(x+PApplet.parseInt(wF/2), y_pad+PApplet.parseInt(hF/2), digitalReadDotHeight, digitalReadDotHeight);
      digitalReadDots[4].screenResized(x+PApplet.parseInt(wF*(7.0f/8.0f)), y_pad+PApplet.parseInt(hF*(7.0f/8.0f)), digitalReadDotHeight, digitalReadDotHeight);
      digitalReadDots[1].screenResized(digitalReadDots[0].DotX+PApplet.parseInt(wF*(3.0f/16.0f)), digitalReadDots[0].DotY+PApplet.parseInt(hF*(3.0f/16.0f)), digitalReadDotHeight, digitalReadDotHeight);
      digitalReadDots[3].screenResized(digitalReadDots[2].DotX+PApplet.parseInt(wF*(3.0f/16.0f)), digitalReadDots[2].DotY+PApplet.parseInt(hF*(3.0f/16.0f)), digitalReadDotHeight, digitalReadDotHeight);

    }

    digitalModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (digitalModeButton.isMouseHere()) {
      digitalModeButton.setIsActive(true);
    }
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(digitalModeButton.isActive && digitalModeButton.isMouseHere()){
      // println("digitalModeButton...");
      if(cyton.isPortOpen()) {
        if (cyton.getBoardMode() != BOARD_MODE_DIGITAL) {
          cyton.setBoardMode(BOARD_MODE_DIGITAL);
          if (cyton.isWifi()) {
            output("Starting to read digital inputs on pin marked D11, D12 and D17");
          } else {
            output("Starting to read digital inputs on pin marked D11, D12, D13, D17 and D18");
          }
        } else {
          cyton.setBoardMode(BOARD_MODE_DEFAULT);
          output("Starting to read accelerometer");
        }
      }
    }
    digitalModeButton.setIsActive(false);
  }
};

//========================================================================================================================
//                      Analog Voltage BAR CLASS -- Implemented by Analog Read Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
class DigitalReadDot{

  int digitalInputPin;
  int digitalInputVal;
  String digitalInputString;
  int padding;
  boolean isOn; //true means data is streaming and channel is active on hardware ... this will send message to OpenBCI Hardware

  TextBox digitalValue;
  TextBox digitalPin;

  boolean drawDigitalValue;

  int dotStroke = 0xffd2d2d2;
  int dot0Fill = 0xfff5f5f5;
  int dot1Fill = 0xfff5f5f5;
  int val0Fill = 0xff000000;
  int val1Fill = 0xffffffff;

  int DotX;
  int DotY;
  int DotWidth;
  int DotHeight;
  float DotCorner;

  DigitalReadDot(PApplet _parent, int _digitalInputPin, int _x, int _y, int _w, int _h, int _padding){ // channel number, x/y location, height, width

    digitalInputPin = _digitalInputPin;
    digitalInputString = str(digitalInputPin);
    digitalInputVal = 0;
    isOn = true;

    if (digitalInputPin == 11) {
      dot1Fill = channelColors[0];
    } else if (digitalInputPin == 12) {
      dot1Fill = channelColors[1];
    } else if (digitalInputPin == 13) {
      dot1Fill = channelColors[2];
    } else if (digitalInputPin == 17) {
      dot1Fill = channelColors[3];
    } else { // 18
      dot1Fill = channelColors[4];
    }

    DotX = _x;
    DotY = _y;
    DotWidth = _w;
    DotHeight = _h;
    padding = _padding;

    digitalValue = new TextBox("", DotX, DotY);
    digitalValue.textColor = color(val0Fill);
    digitalValue.alignH = CENTER;
    digitalValue.alignV = CENTER;

    digitalPin = new TextBox("D" + digitalInputString, DotX, DotY - DotWidth);
    digitalPin.textColor = color(bgColor);
    digitalPin.alignH = CENTER;
    // digitalPin.alignV = CENTER;

    drawDigitalValue = true;
  }

  public void update(){
    //update the voltage values
    if (digitalInputPin == 11) {
      digitalInputVal = (hub.validAccelValues[0] & 0xFF00) >> 8;
    } else if (digitalInputPin == 12) {
      digitalInputVal = hub.validAccelValues[0] & 0xFF;
    } else if (digitalInputPin == 13) {
      digitalInputVal = (hub.validAccelValues[1] & 0xFF00) >> 8;
    } else if (digitalInputPin == 17) {
      digitalInputVal = hub.validAccelValues[1] & 0xFF;
    } else { // 18
      digitalInputVal = hub.validAccelValues[2];
    }

    digitalValue.string = String.format("%d", digitalInputVal);
  }

  public void draw(){
    pushStyle();

    //draw plot

    if (digitalInputVal == 1) {
      fill(dot1Fill);
      digitalValue.textColor = val1Fill;
    } else {
      fill(dot0Fill);
      digitalValue.textColor = val0Fill;
    }
    stroke(dotStroke);
    ellipse(DotX, DotY, DotWidth, DotHeight);

    if(drawDigitalValue){
      digitalValue.draw();
      digitalPin.draw();
    }

    popStyle();
  }

  public void screenResized(int _x, int _y, int _w, int _h){
    DotX = _x;
    DotY = _y;
    DotWidth = _w;
    DotHeight = _h;
    DotCorner = (sqrt(2)*DotWidth/2)/2;

    // println("DigitalReadDot: " + digitalInputPin + " screenResized: DotX: " + DotX + " DotY: " + DotY + " DotWidth: "+ DotWidth + " DotHeight: " + DotHeight);

    digitalPin.x = DotX;
    digitalPin.y = DotY - PApplet.parseInt(DotWidth/2.0f);

    digitalValue.x = DotX;
    digitalValue.y = DotY;
  }
};
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
    addDropdown("minUVRange", "Min \u0394uV", Arrays.asList("10 uV", "20 uV", "40 uV", "80 uV"), 1);

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
      cfc.switchTripped = true;
      cfc.timeOfLastTrip = millis();
      cfc.switchCounter++;
    }
    if (switchTripped && output_normalized <= untripThreshold) {
      //Untripped
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

//fft global variables
// int Nfft; //125Hz, 200Hz, 250Hz -> 256points. 1000Hz -> 1024points. 1600Hz -> 2048 points.  //prev: Use N=256 for normal, N=512 for MU waves
// float fs_Hz; // AJ Keller removed because shall get sample rate at runtime
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

  int[] xLimOptions = {20, 40, 60, 100, 120, 250, 500, 800};
  int[] yLimOptions = {10, 50, 100, 1000};

  int xLim = xLimOptions[2];  //maximum value of x axis ... in this case 20 Hz, 40 Hz, 60 Hz, 120 Hz
  int xMax = xLimOptions[xLimOptions.length-1];   //maximum possible frequency in FFT
  int FFT_indexLim = PApplet.parseInt(1.0f*xMax*(getNfftSafe()/getSampleRateSafe()));   // maxim value of FFT index
  int yLim = yLimOptions[2];  //maximum value of y axis ... 100 uV

  W_fft(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    //This is the protocol for setting up dropdowns.
    //Note that these 3 dropdowns correspond to the 3 global functions below
    //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
    addDropdown("MaxFreq", "Max Freq", Arrays.asList("20 Hz", "40 Hz", "60 Hz", "100 Hz", "120 Hz", "250 Hz", "500 Hz", "800 Hz"), 2);
    addDropdown("VertScale", "Max uV", Arrays.asList("10 uV", "50 uV", "100 uV", "1000 uV"), 2);
    addDropdown("LogLin", "Log/Lin", Arrays.asList("Log", "Linear"), 0);
    addDropdown("Smoothing", "Smooth", Arrays.asList("0.0", "0.5", "0.75", "0.9", "0.95", "0.98"), smoothFac_ind); //smoothFac_ind is a global variable at the top of W_headPlot.pde
    addDropdown("UnfiltFilt", "Filters?", Arrays.asList("Filtered", "Unfilt."), 0);

    fft_points = new GPointsArray[nchan];
    // println("fft_points.length: " + fft_points.length);
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
    float sr = getSampleRateSafe();
    int nfft = getNfftSafe();

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
        // float a = getSampleRateSafe();
        // float aa = fftBuff[i].getBand(j);
        // float b = fftBuff[i].getBand(j);
        // float c = Nfft;

        powerAtBin = new GPoint((1.0f*sr/nfft)*j, fftBuff[i].getBand(j));
        fft_points[i].set(j, powerAtBin);
        // GPoint powerAtBin = new GPoint((1.0*getSampleRateSafe()/Nfft)*j, fftBuff[i].getBand(j));

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
//    W_focus.pde (ie "Focus Widget")
//
//    This widget helps you visualize the alpha and beta value and the calculated focused state
//    You can ask a robot to press Up Arrow key stroke whenever you are focused.
//    You can also send the focused state to Arduino
//
//    Created by: Wangshu Sun, August 2016
//
///////////////////////////////////////////////////,





// color enums
public enum FocusColors {
  GREEN, CYAN, ORANGE
}

class W_Focus extends Widget {
  //to see all core variables/methods of the Widget class, refer to Widget.pde
  Robot robot;    // a key-stroking robot waiting for focused state
  boolean enableKey = false;  // enable key stroke by the robot
  int keyNum = 0; // 0 - up arrow, 1 - Spacebar
  boolean enableSerial = false; // send the Focused state to Arduino

  // output values
  float alpha_avg = 0, beta_avg = 0;
  boolean isFocused;

  // alpha, beta threshold default values
  float alpha_thresh = 0.7f, beta_thresh = 0.55f, alpha_upper = 2, beta_upper = 2;

  // drawing parameters
  boolean showAbout = false;
  PFont myfont = createFont("fonts/Raleway-SemiBold.otf", 12);
  PFont myfont1 = createFont("fonts/Raleway-SemiBold.otf", 300);
  PFont f = createFont("Arial Bold", 24); //for widget title

  FocusColors focusColors = FocusColors.GREEN;

  int cBack, cDark, cMark, cFocus, cWave, cPanel;

  // float x, y, w, h;  //widget topleft xy, width and height
  float xc, yc, wc, hc; // crystal ball center xy, width and height
  float wg, hg;  //graph width, graph height
  float wl;  // line width
  float xg1, yg1;  //graph1 center xy
  float xg2, yg2;  //graph1 center xy
  float rp;  // padding radius
  float rb;  // button radius
  float xb, yb; // button center xy
  
  int timeOfLastTrip = 0;
  int timeOfLastOff = 0;

  // two sliders for alpha and one slider for beta
  FocusSlider sliderAlphaMid, sliderBetaMid;
  FocusSlider_Static sliderAlphaTop;

  W_Focus(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    // initialize graphics parameters
    onColorChange();
    update_graphic_parameters();

    // sliders
    sliderAlphaMid = new FocusSlider(x + xg1 + wg * 0.8f, y + yg1 + hg/2, y + yg1 - hg/2, alpha_thresh / alpha_upper);
    sliderAlphaTop = new FocusSlider_Static(x + xg1 + wg * 0.8f, y + yg1 + hg/2, y + yg1 - hg/2);
    sliderBetaMid = new FocusSlider(x + xg2 + wg * 0.8f, y + yg2 + hg/2, y + yg2 - hg/2, beta_thresh / beta_upper);

    //Dropdowns.
    addDropdown("ChooseFocusColor", "Theme", Arrays.asList("Green", "Orange", "Cyan"), 0);
    addDropdown("StrokeKeyWhenFocused", "KeyPress", Arrays.asList("OFF", "UP", "SPACE"), 0);
    addDropdown("SerialSendFocused", "Serial", Arrays.asList("OFF", "ON"), 0);

    // prepare simulate keystroking
    try {
      robot = new Robot();
    } catch (AWTException e) {
      e.printStackTrace();
      exit();
    }

  }

  public void onColorChange() {
    switch(focusColors) {
      case GREEN:
        cBack = 0xffffffff;   //white
        cDark = 0xff3068a6;   //medium/dark blue
        cMark = 0xff4d91d9;    //lighter blue
        cFocus = 0xffb8dc69;   //theme green
        cWave = 0xffffdd3a;    //yellow
        cPanel = 0xfff5f5f5;   //little grey
        break;
      case ORANGE:
        cBack = 0xffffffff;   //white
        cDark = 0xff377bc4;   //medium/dark blue
        cMark = 0xff5e9ee2;    //lighter blue
        cFocus = 0xfffcce51;   //orange
        cWave = 0xffffdd3a;    //yellow
        cPanel = 0xfff5f5f5;   //little grey
        break;
      case CYAN:
        cBack = 0xffffffff;   //white
        cDark = 0xff377bc4;   //medium/dark blue
        cMark = 0xff5e9ee2;    //lighter blue
        cFocus = 0xff91f4fc;   //cyan
        cWave = 0xffffdd3a;    //yellow
        cPanel = 0xfff5f5f5;   //little grey
        break;
    }
  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    updateFocusState(); // focus calculation
    invokeKeyStroke();  // robot keystroke
    sendFocusSerial();  // send focus data to serial port

    // update sliders
    sliderAlphaMid.update();
    sliderAlphaTop.update();
    sliderBetaMid.update();

    // update threshold values
    alpha_thresh = alpha_upper * sliderAlphaMid.getVal();
    beta_thresh = beta_upper * sliderBetaMid.getVal();

    alpha_upper = sliderAlphaTop.getVal() * 2;
    beta_upper = alpha_upper;

    sliderAlphaMid.setVal(alpha_thresh / alpha_upper);
    sliderBetaMid.setVal(beta_thresh / beta_upper);
  }

  public void updateFocusState() {
    
    int timeToWaitThresh = 3000;
    int timeToWaitOffThresh = 1000;
    
    // focus detection algorithm based on Jordan's clean mind: focus == high alpha average && low beta average
    float FFT_freq_Hz, FFT_value_uV;
    int alpha_count = 0, beta_count = 0;

    for (int Ichan=0; Ichan < 7; Ichan++) {  // only consider first two channels
      for (int Ibin=0; Ibin < fftBuff[Ichan].specSize(); Ibin++) {
        FFT_freq_Hz = fftBuff[Ichan].indexToFreq(Ibin);
        FFT_value_uV = fftBuff[Ichan].getBand(Ibin);

        if (FFT_freq_Hz >= 7.5f && FFT_freq_Hz <= 12.5f) { //FFT bins in alpha range
         alpha_avg += FFT_value_uV;
         alpha_count ++;
        }
        else if (FFT_freq_Hz > 12.5f && FFT_freq_Hz <= 30) {  //FFT bins in beta range
          beta_avg += FFT_value_uV;
          beta_count ++;
        }
      }
    }

    alpha_avg = alpha_avg / alpha_count;  // average uV per bin
    //alpha_avg = alpha_avg / (cyton.getSampleRate()/Nfft);  // average uV per delta freq
    beta_avg = beta_avg / beta_count;  // average uV per bin
    //beta_avg = beta_avg / (cyton.getSampleRate()/Nfft);  // average uV per delta freq
    //current time = int(float(currentTableRowIndex)/cyton.getSampleRate());

    // version 1
    if (alpha_avg > alpha_thresh && alpha_avg < alpha_upper && beta_avg < beta_thresh) {
      
      
      isFocused = true;
      if(millis() - timeOfLastTrip >= timeToWaitThresh){
        timeOfLastTrip = millis();
        println(timeOfLastTrip);
        println(millis());
        println(timeToWaitThresh);
        
        voice = soundMinim.loadFile(dataPath("m_yes.mp3"));
        voice.play();
      }
    } else {
      isFocused = false;
    }

    //alpha_avg = beta_avg = 0;

  }

  public void invokeKeyStroke() {
    // robot keystroke
    if (enableKey) {
      if (keyNum == 0) {
        if (isFocused) {
          robot.keyPress(KeyEvent.VK_UP);    //if you want to change to other key, google "java keyEvent" to see the full list
        }
        else {
          robot.keyRelease(KeyEvent.VK_UP);
        }
      }
      else if (keyNum == 1) {
        if (isFocused) {
          robot.keyPress(KeyEvent.VK_SPACE);    //if you want to change to other key, google "java keyEvent" to see the full list
        }
        else {
          robot.keyRelease(KeyEvent.VK_SPACE);
        }
      }
    }
  }

  public void sendFocusSerial() {
    // ----------- if turned on, send the focused state to Arduino via serial port -----------
    if (enableSerial) {
      try {
        serial_output.write(PApplet.parseInt(isFocused) + 48);
        serial_output.write('\n');
      }
      catch(RuntimeException e) {
        if (isVerbose) println("serial not present, search 'serial_output' in OpenBCI.pde and check serial settings.");
      }
    }
  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

    //----------------- presettings before drawing Focus Viz --------------
    translate(x, y);
    textAlign(CENTER, CENTER);
    textFont(myfont1);

    //----------------- draw background rectangle and panel -----------------
    fill(cBack);
    noStroke();
    rect(0, 0, w, h);

    fill(cPanel);
    noStroke();
    rect(rp, rp, w-rp*2, h-rp*2);

    //----------------- draw focus crystalball -----------------
    noStroke();
    if (isFocused) {
      fill(cFocus);
      stroke(cFocus);
    } else {
      fill(cDark);
    }
    //ellipse(xc, yc, wc, hc);
    noStroke();
    // draw focus label
    if (isFocused) {
      fill(cFocus);
      //text("YES", xc, yc + hc/2 + 16);
      textSize(h/2); 
      text("YES", xc, yc );
    } else {
      fill(cMark);
      //text("YES", xc, yc + hc/2 + 16);
      textSize(h/2); 
      text("YES", xc, yc);
    }
    
    textFont(myfont);
    //----------------- draw alpha meter -----------------
    noStroke();
    fill(cDark);
    rect(xg1 - wg/2, yg1 - hg/2, wg, hg);

    float hat = map(alpha_thresh, 0, alpha_upper, 0, hg);  // alpha threshold height
    stroke(cMark);
    line(xg1 - wl/2, yg1 + hg/2, xg1 + wl/2, yg1 + hg/2);
    line(xg1 - wl/2, yg1 - hg/2, xg1 + wl/2, yg1 - hg/2);
    line(xg1 - wl/2, yg1 + hg/2 - hat, xg1 + wl/2, yg1 + hg/2 - hat);

    // draw alpha zone and text
    noStroke();
    if (alpha_avg > alpha_thresh && alpha_avg < alpha_upper) {
      fill(cFocus);
    } else {
      fill(cMark);
    }
    rect(xg1 - wg/2, yg1 - hg/2, wg, hg - hat);
    text("alpha", xg1, yg1 + hg/2 + 16);

    // draw connection between two sliders
    stroke(cMark);
    line(xg1 + wg * 0.8f, yg1 - hg/2 + 10, xg1 + wg * 0.8f, yg1 + hg/2 - hat - 10);

    noStroke();
    fill(cMark);
    text(String.format("%.01f", alpha_upper), xg1 - wl/2 - 14, yg1 - hg/2);
    text(String.format("%.01f", alpha_thresh), xg1 - wl/2 - 14, yg1 + hg/2 - hat);
    text("0.0", xg1 - wl/2 - 14, yg1 + hg/2);

    stroke(cWave);
    strokeWeight(4);
    float ha = map(alpha_avg, 0, alpha_upper, 0, hg);  //alpha height
    ha = constrain(ha, 0, hg);
    line(xg1 - wl/2, yg1 + hg/2 - ha, xg1 + wl/2, yg1 + hg/2 - ha);
    strokeWeight(1);

    //----------------- draw beta meter -----------------
    noStroke();
    fill(cDark);
    rect(xg2 - wg/2, yg2 - hg/2, wg, hg);

    float hbt = map(beta_thresh, 0, beta_upper, 0, hg);  // beta threshold height
    stroke(cMark);
    line(xg2 - wl/2, yg2 + hg/2, xg2 + wl/2, yg2 + hg/2);
    line(xg2 - wl/2, yg2 - hg/2, xg2 + wl/2, yg2 - hg/2);
    line(xg2 - wl/2, yg2 + hg/2 - hbt, xg2 + wl/2, yg2 + hg/2 - hbt);

    // draw beta zone and text
    noStroke();
    if (beta_avg < beta_thresh) {
      fill(cFocus);
    } else {
      fill(cMark);
    }
    rect(xg2 - wg/2, yg2 + hg/2 - hbt, wg, hbt);
    text("beta", xg2, yg2 + hg/2 + 16);

    // draw connection between slider and bottom
    stroke(cMark);
    float yt = yg2 + hg/2 - hbt + 10;   // y threshold
    yt = constrain(yt, yg2 - hg/2 + 10, yg2 + hg/2);
    line(xg2 + wg * 0.8f, yg2 + hg/2, xg2 + wg * 0.8f, yt);

    noStroke();
    fill(cMark);
    text(String.format("%.01f", beta_upper), xg2 - wl/2 - 14, yg2 - hg/2);
    text(String.format("%.01f", beta_thresh), xg2 - wl/2 - 14, yg2 + hg/2 - hbt);
    text("0.0", xg2 - wl/2 - 14, yg2 + hg/2);

    stroke(cWave);
    strokeWeight(4);
    float hb = map(beta_avg, 0, beta_upper, 0, hg);  //beta height
    hb = constrain(hb, 0, hg);
    line(xg2 - wl/2, yg2 + hg/2 - hb, xg2 + wl/2, yg2 + hg/2 - hb);
    strokeWeight(1);

    translate(-x, -y);

    //------------------ draw sliders --------------------
    sliderAlphaMid.draw();
    sliderAlphaTop.draw();
    sliderBetaMid.draw();

    //----------------- draw about button -----------------
    translate(x, y);
    if (showAbout) {
      stroke(cDark);
      fill(cBack);

      rect(rp, rp, w-rp*2, h-rp*2);
      textAlign(LEFT, TOP);
      fill(cDark);
      text("This widget recognizes a focused mental state by looking at alpha and beta wave levels on channel 1 & 2. For better result, try setting the smooth at 0.98 in FFT plot.\n\nThe algorithm thinks you are focused when the alpha level is between 0.7~2uV and the beta level is between 0~0.7 uV, otherwise it thinks you are not focused. It is designed based on Jordan Frand\u2019s brainwave and tested on other subjects, and you can playback Jordan's file in W_Focus folder.\n\nYou can turn on KeyPress and use your focus play a game, so whenever you are focused, the specified UP arrow or SPACE key will be pressed down, otherwise it will be released. You can also try out the Arduino output feature, example and instructions are included in W_Focus folder. For more information, contact wangshu.sun@hotmail.com.", rp*1.5f, rp*1.5f, w-rp*3, h-rp*3);
    }
    // draw the button that toggles information
    noStroke();
    fill(cDark);
    ellipse(xb, yb, rb, rb);
    fill(cBack);
    textAlign(CENTER, CENTER);
    if (showAbout) {
      text("x", xb, yb);
    } else {
      text("?", xb, yb);
    }

    //----------------- revert origin point of draw to default -----------------
    translate(-x, -y);
    textAlign(LEFT, BASELINE);

    popStyle();

  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    //put your code here...
    update_graphic_parameters();

    //update sliders...
    sliderAlphaMid.screenResized(x + xg1 + wg * 0.8f, y + yg1 + hg/2, y + yg1 - hg/2);
    sliderAlphaTop.screenResized(x + xg1 + wg * 0.8f, y + yg1 + hg/2, y + yg1 - hg/2);
    sliderBetaMid.screenResized(x + xg2 + wg * 0.8f, y + yg2 + hg/2, y + yg2 - hg/2);
  }

  public void update_graphic_parameters () {
    xc = w/4;
    yc = h/2;
    wc = w/4;
    hc = w/4;
    wg = 0.07f*w;
    hg = 0.64f*h;
    wl = 0.11f*w;
    xg1 = 0.6f*w;
    yg1 = 0.5f*h;
    xg2 = 0.83f*w;
    yg2 = 0.5f*h;
    rp = max(w*0.05f, h*0.05f);
    rb = 20;
    xb = w-rp;
    yb = rp;
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //  about button
    if (dist(mouseX,mouseY,xb+x,yb+y) <= rb) {
      showAbout = !showAbout;
    }

    // sliders
    sliderAlphaMid.mousePressed();
    sliderAlphaTop.mousePressed();
    sliderBetaMid.mousePressed();
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    // sliders
    sliderAlphaMid.mouseReleased();
    sliderAlphaTop.mouseReleased();
    sliderBetaMid.mouseReleased();
  }

};

/* ---------------------- Supporting Slider Classes ---------------------------*/

// abstract basic slider
public abstract class BasicSlider {
  float x, y, w, h;  // center x, y. w, h means width and height of triangle
  float yBot, yTop;   // y range. Notice val of top y is less than bottom y
  boolean isPressed = false;
  int cNormal = 0xffCCCCCC;
  int cPressed = 0xffFF0000;

  BasicSlider(float _x, float _yBot, float _yTop) {
    x = _x;
    yBot = _yBot;
    yTop = _yTop;
    w = 10;
    h = 10;
  }

  // abstract functions

  public abstract void update();
  public abstract void screenResized(float _x, float _yBot, float _yTop);
  public abstract float getVal();
  public abstract void setVal(float _val);

  // shared functions

  public void draw() {
    if (isPressed) fill(cPressed);
    else fill(cNormal);
    noStroke();
    triangle(x-w/2, y, x+w/2, y-h/2, x+w/2, y+h/2);
  }

  public void mousePressed() {
    if (abs(mouseX - (x)) <= w/2 && abs(mouseY - y) <= h/2) {
      isPressed = true;
    }
  }

  public void mouseReleased() {
    if (isPressed) {
      isPressed = false;
    }
  }
}

// middle slider that changes value and move
public class FocusSlider extends BasicSlider {
  private float val = 0;  // val = 0 ~ 1 -> yBot to yTop
  final float valMin = 0;
  final float valMax = 0.90f;
  FocusSlider(float _x, float _yBot, float _yTop, float _val) {
    super(_x, _yBot, _yTop);
    val = constrain(_val, valMin, valMax);
    y = map(val, 0, 1, yBot, yTop);
  }

  public void update() {
    if (isPressed) {
      float newVal = map(mouseY, yBot, yTop, 0, 1);
      val = constrain(newVal, valMin, valMax);
      y = map(val, 0, 1, yBot, yTop);
      println(val);
    }
  }

  public void screenResized(float _x, float _yBot, float _yTop) {
    x = _x;
    yBot = _yBot;
    yTop = _yTop;
    y = map(val, 0, 1, yBot, yTop);
  }

  public float getVal() {
     return val;
  }

  public void setVal(float _val) {
     val = constrain(_val, valMin, valMax);
     y = map(val, 0, 1, yBot, yTop);
  }
}

// top slider that changes value but doesn't move
public class FocusSlider_Static extends BasicSlider {
  private float val = 0;  // val = 0 ~ 1 -> yBot to yTop
  final float valMin = 0.5f;
  final float valMax = 5.0f;
  FocusSlider_Static(float _x, float _yBot, float _yTop) {
    super(_x, _yBot, _yTop);
    val = 1;
    y = yTop;
  }

  public void update() {
    if (isPressed) {
      float diff = map(mouseY, yBot, yTop, -0.07f, 0);
      val = constrain(val + diff, valMin, valMax);
      println(val);
    }
  }

  public void screenResized(float _x, float _yBot, float _yTop) {
    x = _x;
    yBot = _yBot;
    yTop = _yTop;
    y = yTop;
  }

  public float getVal() {
     return val;
  }

  public void setVal(float _val) {
     val = constrain(_val, valMin, valMax);
  }

}

/* ---------------- Global Functions For Menu Entries --------------------*/

// //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
public void StrokeKeyWhenFocused(int n){
  // println("Item " + (n+1) + " selected from Dropdown 1");
  if(n==0){
    //do this
    w_focus.enableKey = false;
    println("The robot ignores focused state and will not press any key.");
  } else if(n==1){
    //do this instead
    w_focus.enableKey = true;
    w_focus.keyNum = 0;
    println("The robot will keep pressing Arrow Up key when you are focused, and release the key when you lose focus.");
  } else if(n==2){
    //do this instead
    w_focus.enableKey = true;
    w_focus.keyNum = 1;
    println("The robot will keep pressing Spacebar when you are focused, and release the key when you lose focus.");
  }

  closeAllDropdowns(); // do this at the end of all widget-activated functions to ensure proper widget interactivity ... we want to make sure a click makes the menu close
}

public void SerialSendFocused(int n){
  if(n==0){
    //do this
    w_focus.enableSerial = false;
    println("Serial write off.");
  } else if(n==1){
    //do this instead
    w_focus.enableSerial = true;
    println("Serial write on, writing character 1 (int 49) when focused, and character 0 (int 48) when losing focus.");
    println("Current output port name: " + serial_output_portName + ". Current baud rate: " + serial_output_baud + ".");
    println("You can change serial settings in OpenBCI_GUI.pde by searching serial_output.");
  }
  closeAllDropdowns();
}

public void ChooseFocusColor(int n){
  if(n==0){
    w_focus.focusColors = FocusColors.GREEN;
    w_focus.onColorChange();
  } else if(n==1){
    w_focus.focusColors = FocusColors.ORANGE;
    w_focus.onColorChange();
  } else if(n==2){
    w_focus.focusColors = FocusColors.CYAN;
    w_focus.onColorChange();
  }
  closeAllDropdowns();
}

////////////////////////////////////////////////////
//
//  W_MarkerMode is used to put the board into marker mode
//  by Gerrie van Zyl
//  Basd on W_Analogread by AJ Keller
//
//
///////////////////////////////////////////////////,

class W_MarkerMode extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...

  // color boxBG;
  int graphStroke = 0xffd2d2d2;
  int graphBG = 0xfff5f5f5;
  int textColor = 0xff000000;

  int strokeColor;

  // Accelerometer Stuff
  int MarkerBuffSize = 500; //points registered in accelerometer buff

  int padding = 30;

  // bottom xyz graph
  int MarkerWindowWidth;
  int MarkerWindowHeight;
  int MarkerWindowX;
  int MarkerWindowY;


  int eggshell;
  int Xcolor;

  float yMaxMin;

  float currentXvalue;

  int[] X;

  int lastMarker=0;
  int localValidLastMarker;

  float dummyX;

  // for the synthetic markers
  float synthTime;
  int synthCount;

  boolean OBCI_inited= true;

  Button markerModeButton;

  W_MarkerMode(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

    // boxBG = bgColor;
    strokeColor = color(138, 146, 153);

    // Marker Sensor Stuff
    eggshell = color(255, 253, 248);
    Xcolor = color(224, 56, 45);


    setGraphDimensions();

    // The range of markers
    yMaxMin = 256;

    // XYZ buffer for bottom graph
    X = new int[MarkerBuffSize];

    // for synthesizing values
    synthTime = 0.0f;
    
    markerModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn MarkerMode On", 12);
    markerModeButton.setCornerRoundess((int)(navHeight-6));
    markerModeButton.setFont(p6,10);
    markerModeButton.setColorNotPressed(color(57,128,204));
    markerModeButton.textColorNotActive = color(255);
    markerModeButton.hasStroke(false);
    markerModeButton.setHelpText("Click this button to activate/deactivate the MarkerMode of your Cyton board!");
  }

  public void initPlayground(Cyton _OBCI) {
    OBCI_inited = true;
  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    localValidLastMarker =  hub.validLastMarker;  // make a local copy so it can be manipulated in SYNTHETIC mode
    hub.validLastMarker = 0;

    if (eegDataSource == DATASOURCE_SYNTHETIC) {
      localValidLastMarker = synthesizeMarkerData();
    }
    if (eegDataSource == DATASOURCE_CYTON || eegDataSource == DATASOURCE_SYNTHETIC) {
      if (isRunning && cyton.getBoardMode() == BOARD_MODE_MARKER) {
        if (localValidLastMarker > 0){
          lastMarker = localValidLastMarker;  // this holds the last marker for the display
        } 
        X[X.length-1] =
          PApplet.parseInt(map(logScaleMarker(localValidLastMarker), 0, yMaxMin, PApplet.parseFloat(MarkerWindowY+MarkerWindowHeight), PApplet.parseFloat(MarkerWindowY)));
        X[X.length-1] = constrain(X[X.length-1], MarkerWindowY, MarkerWindowY+MarkerWindowHeight);

        shiftWave();
      }
    } else {  // playback data
      currentXvalue = accelerometerBuff[0][accelerometerBuff[0].length-1];
    }
  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    pushStyle();
    //put your code here...
    //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    if (true) {

      fill(50);
      textFont(p4, 14);
      textAlign(CENTER,CENTER);

      fill(graphBG);
      stroke(graphStroke);
      rect(MarkerWindowX, MarkerWindowY, MarkerWindowWidth, MarkerWindowHeight);
      line(MarkerWindowX, MarkerWindowY + MarkerWindowHeight/2, MarkerWindowX+MarkerWindowWidth, MarkerWindowY + MarkerWindowHeight/2); //midline

      fill(50);
      textFont(p5, 12);
      textAlign(CENTER,CENTER);
      text((int)yMaxMin, MarkerWindowX+MarkerWindowWidth + 12, MarkerWindowY);
      text((int)16, MarkerWindowX+MarkerWindowWidth + 12, MarkerWindowY + MarkerWindowHeight/2);
      text("0", MarkerWindowX+MarkerWindowWidth + 12, MarkerWindowY + MarkerWindowHeight);


      fill(graphBG);  // pulse window background
      stroke(graphStroke);

      stroke(180);

      fill(50);
      textFont(p3, 16);

      if (eegDataSource == DATASOURCE_CYTON || eegDataSource == DATASOURCE_SYNTHETIC) {  // LIVE
        markerModeButton.draw();
        drawMarkerValues();
        drawMarkerWave();
      }
      else {  // PLAYBACK
        drawMarkerValues();
        drawMarkerWave2();
      }
    }
    popStyle();
  }

  public void setGraphDimensions(){
    MarkerWindowWidth = w - padding*2;
    MarkerWindowHeight = PApplet.parseInt((PApplet.parseFloat(h) - PApplet.parseFloat(padding*3)));
    MarkerWindowX = x + padding;
    MarkerWindowY = y + h - MarkerWindowHeight - padding;

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
    println("Acc Widget -- Screen Resized.");

    setGraphDimensions();

    //empty arrays to start redrawing from scratch
    for (int i=0; i<X.length; i++) {  // initialize the accelerometer data
      X[i] = MarkerWindowY + MarkerWindowHeight; // X at 1/4
    }

    markerModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (markerModeButton.isMouseHere()) {
      markerModeButton.setIsActive(true);
    }
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(markerModeButton.isActive && markerModeButton.isMouseHere()){
      // println("markerModeButton...");
      if((cyton.isPortOpen() && eegDataSource == DATASOURCE_CYTON) || eegDataSource == DATASOURCE_SYNTHETIC) {
        if (cyton.getBoardMode() != BOARD_MODE_MARKER) {
          cyton.setBoardMode(BOARD_MODE_MARKER);
          output("Starting to read markers");
          markerModeButton.setString("Turn Marker Off");
        } else {
          cyton.setBoardMode(BOARD_MODE_DEFAULT);
          output("Starting to read accelerometer");
          markerModeButton.setString("Turn Marker On");
        }
      } 
    }
    markerModeButton.setIsActive(false);
  }

  //add custom classes functions here
  public void drawMarkerValues() {
    textAlign(LEFT,CENTER);
    textFont(h1,20);
    fill(Xcolor);
    text("Last Marker = " + lastMarker, x+padding , y + (h/12)*1.5f);
  }

  public void shiftWave() {
    for (int i = 0; i < X.length-1; i++) {      // move the pulse waveform by
      X[i] = X[i+1];
    }
  }

  public void drawMarkerWave() {
    noFill();
    strokeWeight(2);
    beginShape();                                  // using beginShape() renders fast
    stroke(Xcolor);
    for (int i = 0; i < X.length; i++) {
      // int xi = int(map(i, 0, X.length-1, 0, MarkerWindowWidth-1));
      // vertex(MarkerWindowX+xi, X[i]);                    //draw a line connecting the data points
      int xi = PApplet.parseInt(map(i, 0, X.length-1, 0, MarkerWindowWidth-1));
      // int yi = int(map(X[i], yMaxMin, -yMaxMin, 0.0, MarkerWindowHeight-1));
      // int yi = 2;
      vertex(MarkerWindowX+xi, X[i]);                    //draw a line connecting the data points
    }
    endShape();
  }

  public void drawMarkerWave2() {
    noFill();
    strokeWeight(1);
    beginShape();                                  // using beginShape() renders fast
    stroke(Xcolor);
    for (int i = 0; i < accelerometerBuff[0].length; i++) {
      int x = PApplet.parseInt(map(accelerometerBuff[0][i], -yMaxMin, yMaxMin, PApplet.parseFloat(MarkerWindowY+MarkerWindowHeight), PApplet.parseFloat(MarkerWindowY)));  // ss
      x = constrain(x, MarkerWindowY, MarkerWindowY+MarkerWindowHeight);
      vertex(MarkerWindowX+i, x);                    //draw a line connecting the data points
    }
    endShape();
  }

  public int synthesizeMarkerData() {
    synthTime += 0.02f;
    int valueMarker;
    
    if (synthCount++ > 10){      
      valueMarker =  PApplet.parseInt((sin(synthTime) +1.0f)*127.f);
      synthCount = 0;
    } else {
      valueMarker = 0;
    }

    return valueMarker;
  }


  public int logScaleMarker( float value ) {
    // this returns log value between 0 and yMaxMin for a value between 0. and 255.
    return PApplet.parseInt(log(PApplet.parseInt(value)+1.0f)*yMaxMin/log(yMaxMin+1));
  }

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
//    W_PulseSensor.pde
//
//    Created: Joel Murphy, Spring 2017
//
///////////////////////////////////////////////////,

class W_PulseSensor extends Widget {

  //to see all core variables/methods of the Widget class, refer to Widget.pde
  //put your custom variables here...


  int graphStroke = 0xffd2d2d2;
  int graphBG = 0xfff5f5f5;
  int textColor = 0xff000000;

// Pulse Sensor Visualizer Stuff
  int count = 0;
  int heart = 0;
  int PulseBuffSize = dataPacketBuff.length; // Originally 400
  int BPMbuffSize = 100;

  int PulseWindowWidth;
  int PulseWindowHeight;
  int PulseWindowX;
  int PulseWindowY;
  int BPMwindowWidth;
  int BPMwindowHeight;
  int BPMwindowX;
  int BPMwindowY;
  int BPMposX;
  int BPMposY;
  int IBIposX;
  int IBIposY;
  int padding = 15;
  int eggshell;
  int pulseWave;
  int[] PulseWaveY;      // HOLDS HEARTBEAT WAVEFORM DATA
  int[] BPMwaveY;        // HOLDS BPM WAVEFORM DATA
  boolean rising;

  // Synthetic Wave Generator Stuff
  float theta;  // Start angle at 0
  float amplitude;  // Height of wave
  int syntheticMultiplier;
  long thisTime;
  long thatTime;
  int refreshRate;

  // Pulse Sensor Beat Finder Stuff
  // ASSUMES 250Hz SAMPLE RATE
  int[] rate;                    // array to hold last ten IBI values
  int sampleCounter;          // used to determine pulse timing
  int lastBeatTime;           // used to find IBI
  int P =512;                      // used to find peak in pulse wave, seeded
  int T = 512;                     // used to find trough in pulse wave, seeded
  int thresh = 530;                // used to find instant moment of heart beat, seeded
  int amp = 0;                   // used to hold amplitude of pulse waveform, seeded
  boolean firstBeat = true;        // used to seed rate array so we startup with reasonable BPM
  boolean secondBeat = false;      // used to seed rate array so we startup with reasonable BPM
  int BPM;                   // int that holds raw Analog in 0. updated every 2mS
  int Signal;                // holds the incoming raw data
  int IBI = 600;             // int that holds the time interval between beats! Must be seeded!
  boolean Pulse = false;     // "True" when User's live heartbeat is detected. "False" when not a "live beat".
  boolean QS = false;        // becomes true when Arduoino finds a beat.
  int lastProcessedDataPacketInd = 0;
  boolean analogReadOn = false;

  // testing stuff

  Button analogModeButton;



  W_PulseSensor(PApplet _parent){
    super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)



    // Pulse Sensor Stuff
    eggshell = color(255, 253, 248);
    pulseWave = color(224, 56, 45);

    PulseWaveY = new int[PulseBuffSize];
    BPMwaveY = new int[BPMbuffSize];
    rate = new int[10];
    setPulseWidgetVariables();
    initializePulseFinderVariables();

    analogModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Analog Read On", 12);
    analogModeButton.setCornerRoundess((int)(navHeight-6));
    analogModeButton.setFont(p6,10);
    analogModeButton.setColorNotPressed(color(57,128,204));
    analogModeButton.textColorNotActive = color(255);
    analogModeButton.hasStroke(false);
    analogModeButton.setHelpText("Click this button to activate analog reading on the Cyton");

  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    if (curDataPacketInd < 0) return;

    if (eegDataSource == DATASOURCE_CYTON) {  // LIVE FROM CYTON

    } else if (eegDataSource == DATASOURCE_GANGLION) {  // LIVE FROM GANGLION

    } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  // SYNTHETIC

    }
    else {  // PLAYBACK

    }

    int numSamplesToProcess = curDataPacketInd - lastProcessedDataPacketInd;
    if (numSamplesToProcess < 0) {
      numSamplesToProcess += dataPacketBuff.length;
    }
    // Shift internal ring buffer numSamplesToProcess
    if (numSamplesToProcess > 0) {
      for(int i=0; i < PulseWaveY.length - numSamplesToProcess; i++){
        PulseWaveY[i] = PulseWaveY[i+numSamplesToProcess];
      }
    }

    // for each new sample
    int samplesProcessed = 0;
    while (samplesProcessed < numSamplesToProcess) {
      lastProcessedDataPacketInd++;

      // Watch for wrap around
      if (lastProcessedDataPacketInd > dataPacketBuff.length - 1) {
        lastProcessedDataPacketInd = 0;
      }

      int signal = dataPacketBuff[lastProcessedDataPacketInd].auxValues[0];

      processSignal(signal);
      PulseWaveY[PulseWaveY.length - numSamplesToProcess + samplesProcessed] = signal;

      samplesProcessed++;
    }

    if(QS){
      QS = false;
      for(int i=0; i<BPMwaveY.length-1; i++){
        BPMwaveY[i] = BPMwaveY[i+1];
      }
      BPMwaveY[BPMwaveY.length-1] = BPM;
    }

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)


    //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();


    fill(graphBG);
    stroke(graphStroke);
    rect(PulseWindowX,PulseWindowY,PulseWindowWidth,PulseWindowHeight);
    rect(BPMwindowX,BPMwindowY,BPMwindowWidth,BPMwindowHeight);

    fill(50);
    textFont(p4, 16);
    textAlign(LEFT,CENTER);
    text("BPM "+BPM, BPMposX, BPMposY);
    text("IBI "+IBI+"mS", IBIposX, IBIposY);

    if (analogReadOn) {
      drawWaves();
    }

    analogModeButton.draw();

    popStyle();
  }

  public void screenResized(){
    super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

    println("Pulse Sensor Widget -- Screen Resized.");

    setPulseWidgetVariables();
    analogModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    if (analogModeButton.isMouseHere()) {
      analogModeButton.setIsActive(true);
    }
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    if(analogModeButton.isActive && analogModeButton.isMouseHere()){
      // println("analogModeButton...");
      if(cyton.isPortOpen()) {
        if (analogReadOn) {
          cyton.setBoardMode(BOARD_MODE_DEFAULT);
          output("Starting to read accelerometer");
          analogModeButton.setString("Turn Analog Read On");
        } else {
          cyton.setBoardMode(BOARD_MODE_ANALOG);
          output("Starting to read analog inputs on pin marked D11");
          analogModeButton.setString("Turn Analog Read Off");
        }
        analogReadOn = !analogReadOn;
      }
    }
    analogModeButton.setIsActive(false);
  }

  //add custom functions here
  public void setPulseWidgetVariables(){
    PulseWindowWidth = ((w/4)*3) - padding;
    PulseWindowHeight = h - padding *2;
    PulseWindowX = x + padding;
    PulseWindowY = y + h - PulseWindowHeight - padding;

    BPMwindowWidth = w/4 - (padding + padding/2);
    BPMwindowHeight = PulseWindowHeight; // - padding;
    BPMwindowX = PulseWindowX + PulseWindowWidth + padding/2;
    BPMwindowY = PulseWindowY; // + padding;

    BPMposX = BPMwindowX + padding/2;
    BPMposY = y - padding; // BPMwindowHeight + int(float(padding)*2.5);
    IBIposX = PulseWindowX + PulseWindowWidth/2; // + padding/2
    IBIposY = y - padding;

    // float py;
    // float by;
    // for(int i=0; i<PulseWaveY.length; i++){
    //   py = map(float(PulseWaveY[i]),
    //     0.0,1023.0,
    //     float(PulseWindowY + PulseWindowHeight),float(PulseWindowY)
    //   );
    //   PulseWaveY[i] = int(py);
    // }
    // for(int i=0; i<BPMwaveY.length; i++){
    //   BPMwaveY[i] = BPMwindowY + BPMwindowHeight-1;
    // }
  }

  public void initializePulseFinderVariables(){
    sampleCounter = 0;
    lastBeatTime = 0;
    P = 512;
    T = 512;
    thresh = 530;
    amp = 0;
    firstBeat = true;
    secondBeat = false;
    BPM = 0;
    Signal = 512;
    IBI = 600;
    Pulse = false;
    QS = false;

    theta = 0.0f;
    amplitude = 300;
    syntheticMultiplier = 1;

    thatTime = millis();

    // float py = map(float(Signal),
    //   0.0,1023.0,
    //   float(PulseWindowY + PulseWindowHeight),float(PulseWindowY)
    // );
    for(int i=0; i<PulseWaveY.length; i++){
      PulseWaveY[i] = Signal;

      // PulseWaveY[i] = PulseWindowY + PulseWindowHeight/2;
    }
    for(int i=0; i<BPMwaveY.length; i++){
      BPMwaveY[i] = BPM;
    }

  }

  public void drawWaves(){
    int xi, yi;
    noFill();
    strokeWeight(1);
    stroke(pulseWave);
    beginShape();                                  // using beginShape() renders fast
    for(int i=0; i<PulseWaveY.length; i++){
      xi = PApplet.parseInt(map(i,0, PulseWaveY.length-1,0, PulseWindowWidth-1));
      xi += PulseWindowX;
      yi = PApplet.parseInt(map(PulseWaveY[i],0.0f,1023.0f,
        PApplet.parseFloat(PulseWindowY + PulseWindowHeight),PApplet.parseFloat(PulseWindowY)));
      vertex(xi, yi);
    }
    endShape();

    strokeWeight(2);
    stroke(pulseWave);
    beginShape();                                  // using beginShape() renders fast
    for(int i=0; i<BPMwaveY.length; i++){
      xi = PApplet.parseInt(map(i,0, BPMwaveY.length-1,0, BPMwindowWidth-1));
      xi += BPMwindowX;
      yi = PApplet.parseInt(map(BPMwaveY[i], 0.0f,200.0f,
        PApplet.parseFloat(BPMwindowY + BPMwindowHeight), PApplet.parseFloat(BPMwindowY)));
      vertex(xi, yi);
    }
    endShape();

  }

  // THIS IS THE BEAT FINDING FUNCTION
  // BASED ON CODE FROM World Famous Electronics, MAKERS OF PULSE SENSOR
  // https://github.com/WorldFamousElectronics/PulseSensor_Amped_Arduino
  public void processSignal(int sample){                         // triggered when Timer2 counts to 124
    // cli();                                      // disable interrupts while we do this
    // Signal = analogRead(pulsePin);              // read the Pulse Sensor
    sampleCounter += (4 * syntheticMultiplier);                         // keep track of the time in mS with this variable
    int N = sampleCounter - lastBeatTime;       // monitor the time since the last beat to avoid noise

      //  find the peak and trough of the pulse wave
    if(sample < thresh && N > (IBI/5)*3){       // avoid dichrotic noise by waiting 3/5 of last IBI
      if (sample < T){                        // T is the trough
        T = sample;                         // keep track of lowest point in pulse wave
      }
    }

    if(sample > thresh && sample > P){          // thresh condition helps avoid noise
      P = sample;                             // P is the peak
    }                                        // keep track of highest point in pulse wave

    //  NOW IT'S TIME TO LOOK FOR THE HEART BEAT
    // signal surges up in value every time there is a pulse
    if (N > 250){                                   // avoid high frequency noise
      if ( (sample > thresh) && (Pulse == false) && (N > (IBI/5)*3) ){
        Pulse = true;                               // set the Pulse flag when we think there is a pulse
        IBI = sampleCounter - lastBeatTime;         // measure time between beats in mS
        lastBeatTime = sampleCounter;               // keep track of time for next pulse

        if(secondBeat){                        // if this is the second beat, if secondBeat == TRUE
          secondBeat = false;                  // clear secondBeat flag
          for(int i=0; i<=9; i++){             // seed the running total to get a realisitic BPM at startup
            rate[i] = IBI;
          }
        }

        if(firstBeat){                         // if it's the first time we found a beat, if firstBeat == TRUE
          firstBeat = false;                   // clear firstBeat flag
          secondBeat = true;                   // set the second beat flag
          // sei();                               // enable interrupts again
          return;                              // IBI value is unreliable so discard it
        }


        // keep a running total of the last 10 IBI values
        int runningTotal = 0;                  // clear the runningTotal variable

        for(int i=0; i<=8; i++){                // shift data in the rate array
          rate[i] = rate[i+1];                  // and drop the oldest IBI value
          runningTotal += rate[i];              // add up the 9 oldest IBI values
        }

        rate[9] = IBI;                          // add the latest IBI to the rate array
        runningTotal += rate[9];                // add the latest IBI to runningTotal
        runningTotal /= 10;                     // average the last 10 IBI values
        BPM = 60000/runningTotal;               // how many beats can fit into a minute? that's BPM!
        BPM = constrain(BPM,0,200);
        QS = true;                              // set Quantified Self flag
        // QS FLAG IS NOT CLEARED INSIDE THIS FUNCTION
      }
    }

    if (sample < thresh && Pulse == true){   // when the values are going down, the beat is over
      // digitalWrite(blinkPin,LOW);            // turn off pin 13 LED
      Pulse = false;                         // reset the Pulse flag so we can do it again
      amp = P - T;                           // get amplitude of the pulse wave
      thresh = amp/2 + T;                    // set thresh at 50% of the amplitude
      P = thresh;                            // reset these for next time
      T = thresh;
    }

    if (N > 2500){                           // if 2.5 seconds go by without a beat
      thresh = 530;                          // set thresh default
      P = 512;                               // set P default
      T = 512;                               // set T default
      lastBeatTime = sampleCounter;          // bring the lastBeatTime up to date
      firstBeat = true;                      // set these to avoid noise
      secondBeat = false;                    // when we get the heartbeat back
    }

    // sei();                                   // enable interrupts when youre done!
  }// end processSignal


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
    widgetTemplateButton.setURL("http://docs.openbci.com/Tutorials/15-Custom_Widgets");
  }

  public void update(){
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...

  }

  public void draw(){
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();

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

  int[] xLimOptions = {1, 3, 5, 7}; // number of seconds (x axis of graph)
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

    if(eegDataSource == DATASOURCE_CYTON){
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

      if(eegDataSource == DATASOURCE_CYTON){
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

    if(eegDataSource == DATASOURCE_CYTON){
      hardwareSettingsButton.setPos((int)(x0 + 3), (int)(y0 + navHeight + 3));
    }
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)


    if(eegDataSource == DATASOURCE_CYTON){
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

    if(eegDataSource == DATASOURCE_CYTON){
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

    if(eegDataSource == DATASOURCE_CYTON){
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
    if(eegDataSource == DATASOURCE_CYTON){
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
    return numSeconds * (int)getSampleRateSafe();
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

    if(eegDataSource == DATASOURCE_CYTON){
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

    if(eegDataSource == DATASOURCE_CYTON){
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

    if(eegDataSource == DATASOURCE_CYTON){
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


        data[Ichan][i] = (int) (0.5f+ val_uV / cyton.get_scale_fac_uVolts_per_count()); //convert to counts, the 0.5 is to ensure roundi
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

    accelModeButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Accel. On", 12);
    accelModeButton.setCornerRoundess((int)(navHeight-6));
    accelModeButton.setFont(p6,10);
    accelModeButton.setColorNotPressed(color(57,128,204));
    accelModeButton.textColorNotActive = color(255);
    accelModeButton.hasStroke(false);
    accelModeButton.setHelpText("Click this button to activate/deactivate the accelerometer!");
  }

  public void initPlayground(Cyton _OBCI) {
    OBCI_inited = true;
  }

  public float adjustYMaxMinBasedOnSource(){
    float _yMaxMin;
    if(eegDataSource == DATASOURCE_CYTON){
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
      } else if (eegDataSource == DATASOURCE_CYTON) {
        currentXvalue = hub.validAccelValues[0] * cyton.get_scale_fac_accel_G_per_count();
        currentYvalue = hub.validAccelValues[1] * cyton.get_scale_fac_accel_G_per_count();
        currentZvalue = hub.validAccelValues[2] * cyton.get_scale_fac_accel_G_per_count();
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
        currentXvalue = hub.validAccelValues[0] * ganglion.get_scale_fac_accel_G_per_count();
        currentYvalue = hub.validAccelValues[1] * ganglion.get_scale_fac_accel_G_per_count();
        currentZvalue = hub.validAccelValues[2] * ganglion.get_scale_fac_accel_G_per_count();
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

    fill(50);
    textFont(p4, 14);
    textAlign(CENTER,CENTER);
    text("z", PolarWindowX, (PolarWindowY-PolarWindowHeight/2)-12);
    text("x", (PolarWindowX+PolarWindowWidth/2)+8, PolarWindowY-5);
    text("y", (PolarWindowX+PolarCorner)+10, (PolarWindowY-PolarCorner)-10);

    fill(graphBG);
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

    if (eegDataSource == DATASOURCE_CYTON) {  // LIVE
      // fill(Xcolor);
      // text("X " + nf(currentXvalue, 1, 3), x+10, y+40);
      // fill(Ycolor);
      // text("Y " + nf(currentYvalue, 1, 3), x+10, y+80);
      // fill(Zcolor);
      // text("Z " + nf(currentZvalue, 1, 3), x+10, y+120);
      drawAccValues();
      draw3DGraph();
      drawAccWave();
      if (cyton.getBoardMode() != BOARD_MODE_DEFAULT) {
        accelModeButton.setString("Turn Accel On");
        accelModeButton.draw();
      }
    } else if (eegDataSource == DATASOURCE_GANGLION) {
      if (ganglion.isBLE()) accelModeButton.draw();
      drawAccValues();
      draw3DGraph();
      drawAccWave();
    } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  // SYNTHETIC
      drawAccValues();
      draw3DGraph();
      drawAccWave();
    }
    else {  // PLAYBACK
      drawAccValues();
      draw3DGraph();
      drawAccWave2();
    }

    popStyle();
  }

  public void setGraphDimensions(){
    println("accel w "+w);
    println("accel h "+h);
    println("accel x "+x);
    println("accel y "+y);
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
    setGraphDimensions();

    //empty arrays to start redrawing from scratch
    for (int i=0; i<X.length; i++) {  // initialize the accelerometer data
      X[i] = AccelWindowY + AccelWindowHeight/4; // X at 1/4
      Y[i] = AccelWindowY + AccelWindowHeight/2;  // Y at 1/2
      Z[i] = AccelWindowY + (AccelWindowHeight/4)*3;  // Z at 3/4
    }

    accelModeButton.setPos((int)(x + 3), (int)(y + 3 - navHeight));
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    if(eegDataSource == DATASOURCE_GANGLION){
      //put your code here...
      if (ganglion.isBLE()) {
        if (accelModeButton.isMouseHere()) {
          accelModeButton.setIsActive(true);
        }
      }
    } else if (eegDataSource == DATASOURCE_CYTON) {
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
        if(ganglion.isAccelModeActive()){
          ganglion.accelStop();

          accelModeButton.setString("Turn Accel On");
        } else{
          ganglion.accelStart();
          accelModeButton.setString("Turn Accel Off");
        }
      }
      accelModeButton.setIsActive(false);
    } else if (eegDataSource == DATASOURCE_CYTON) {
      if(accelModeButton.isActive && accelModeButton.isMouseHere()){
        cyton.setBoardMode(BOARD_MODE_DEFAULT);
        output("Starting to read accelerometer");
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

    if(isHubInitialized && isHubObjectInitialized && eegDataSource == DATASOURCE_GANGLION){
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
      if(isHubInitialized && isHubObjectInitialized && eegDataSource == DATASOURCE_GANGLION){
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
    headPlot.hp_x = x;
    headPlot.hp_y = y;
    headPlot.hp_w = w;
    headPlot.hp_h = h;
    headPlot.hp_win_x = x;
    headPlot.hp_win_y = y;

    thread("doHardCalcs");
    // headPlot.setPositionSize(x, y, w, h, width, height);     //update position of headplot

  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    //put your code here...
    headPlot.mousePressed();
  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    headPlot.mouseReleased();
  }

  public void mouseDragged(){
    super.mouseDragged(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    //put your code here...
    headPlot.mouseDragged();
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
  // println("BOOOOM!" + n);
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

public void doHardCalcs() {
  if (!w_headPlot.headPlot.threadLock) {
    w_headPlot.headPlot.threadLock = true;
    w_headPlot.headPlot.setPositionSize(w_headPlot.headPlot.hp_x, w_headPlot.headPlot.hp_y, w_headPlot.headPlot.hp_w, w_headPlot.headPlot.hp_h, w_headPlot.headPlot.hp_win_x, w_headPlot.headPlot.hp_win_y);
    w_headPlot.headPlot.hardCalcsDone = true;
    w_headPlot.headPlot.threadLock = false;
  }
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
  private int mouse_over_elec_index = -1;
  private boolean isDragging = false;
  private float drag_x, drag_y;
  public int hp_win_x = 0;
  public int hp_win_y = 0;
  public int hp_x = 0;
  public int hp_y = 0;
  public int hp_w = 0;
  public int hp_h = 0;
  public boolean hardCalcsDone = false;
  public boolean threadLock = false;

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

    hp_x = _x;
    hp_y = _y;
    hp_w = _w;
    hp_h = _h;
    hp_win_x = _win_x;
    hp_win_y = _win_y;
    thread("doHardCalcs");
    // setPositionSize(_x, _y, _w, _h, _win_x, _win_y);
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
    // println("  HeadPlot B 2 0 -- " + millis());

    //find which pixesl are within the head and which pixels are within an electrode
    whereAreThePixels(pixelAddress, withinHead, withinElectrode);
    // println("  HeadPlot B 2 1 -- " + millis());

    //loop over the pixels and make all the connections
    makeAllTheConnections(withinHead, withinElectrode, toPixels, toElectrodes);
    // println("  HeadPlot B 2 3 -- " + millis());

    //compute the pixel values when lighting up each electrode invididually
    for (int Ielec=0; Ielec<n_elec; Ielec++) {
      computeWeightFactorsGivenOneElectrode_iterative(toPixels, toElectrodes, Ielec, weightFac);
    }
    // println("  HeadPlot B 2 4 -- " + millis());

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

  private boolean isMouseOverElectrode(int n){
    float elec_mouse_x_dist = electrode_xy[n][0] - mouseX;
    float elec_mouse_y_dist = electrode_xy[n][1] - mouseY;
    return elec_mouse_x_dist * elec_mouse_x_dist + elec_mouse_y_dist * elec_mouse_y_dist < elec_diam * elec_diam / 4;
  }

  private boolean isDraggedElecInsideHead() {
    int dx = mouseX - circ_x;
    int dy = mouseY - circ_y;
    return dx * dx + dy * dy < (circ_diam - elec_diam) * (circ_diam - elec_diam) / 4;
  }

  public void mousePressed() {
    if (mouse_over_elec_index > -1) {
      isDragging = true;
      drag_x = mouseX - electrode_xy[mouse_over_elec_index][0];
      drag_y = mouseY - electrode_xy[mouse_over_elec_index][1];
    } else {
      isDragging = false;
    }
  }

  public void mouseDragged() {
    if (isDragging && mouse_over_elec_index > -1 && isDraggedElecInsideHead()) {
      electrode_xy[mouse_over_elec_index][0] = mouseX - drag_x;
      electrode_xy[mouse_over_elec_index][1] = mouseY - drag_y;
    }
  }

  public void mouseReleased() {
    isDragging = false;
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
      if (!threadLock && hardCalcsDone) {
        updateHeadVoltages();
        convertVoltagesToHeadImage();
      }
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
    if (!isDragging) {
      mouse_over_elec_index = -1;
    }
    for (int Ielec=0; Ielec < electrode_xy.length; Ielec++) {
      if (drawHeadAsContours) {
        noFill(); //make transparent to allow color to come through from below
      } else {
        fill(electrode_rgb[0][Ielec], electrode_rgb[1][Ielec], electrode_rgb[2][Ielec]);
      }
      if (!isDragging && isMouseOverElectrode(Ielec)) {
        //electrode with a bigger index gets priority in dragging
        mouse_over_elec_index = Ielec;
        strokeWeight(2);
      } else if (mouse_over_elec_index == Ielec) {
        strokeWeight(2);
      } else{
        strokeWeight(1);
      }
      ellipse(electrode_xy[Ielec][0], electrode_xy[Ielec][1], elec_diam, elec_diam); //electrode circle
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
  Boolean connected = false;

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
    
     //connectButton = new Button((int)(x + 3), (int)(y + 3 - navHeight), 120, navHeight - 6, "Turn Analog Read On", 12);
    connectButton = new Button((int)(x) + 2, (int)(y) - navHeight + 2, 58, navHeight - 6, "Connect", fontInfo.buttonLabel_size);
    if (connected){
      connectButton.setColorNotPressed(color(0,255,0));
    }
    else {
      connectButton.setColorNotPressed(color(57,128,204));
    }
    connectButton.textColorNotActive = color(0);
    connectButton.setCornerRoundess((int)(navHeight-6));
    //connectButton.hasStroke(false);
    connectButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
    soundButton = new Button((int)(x) + 65, (int)(y) - navHeight + 2, 45, navHeight - 6, "Voice", fontInfo.buttonLabel_size);
    soundButton.setCornerRoundess((int)(navHeight-6));
    if (speaking){
      soundButton.setColorNotPressed(color(0,255,0));
    }
    else {
      soundButton.setColorNotPressed(color(57,128,204));
    }
    
    if (MatrixAdvanced) {
      if (connectButton != null) connectButton.draw();
      //else connectButton = new Button(int(x) + 2, int(y) - navHeight + 2, 35, navHeight - 6, "Conn", fontInfo.buttonLabel_size);
      if (soundButton != null) soundButton.draw();
      //else soundButton = new Button(int(x) + 60, int(y) - navHeight + 2, 50, navHeight - 6, "Voice", fontInfo.buttonLabel_size);

      stroke(1, 18, 41, 125);

      //if (connectButton != null && connectButton.wasPressed) {
      //  fill(0, 255, 0);
      //  ellipse(x + 50, y - navHeight/2, 8, 16);
      //} else if (connectButton != null && !connectButton.wasPressed) {
      //  fill(255, 0, 0);
      //  ellipse(x + 50, y - navHeight/2, 8, 16);
      //}
        
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
        connectButton.draw();
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
        print("Voice Set to: ");
        println(speaking);
        
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
          connected = true;
          
        }
        catch (Exception e) {
          connectButton.wasPressed = false;
          connected = false;
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
     if (serialChannels[7] == true) {
        playVoice(7);
     }
      if (serialChannels[15] == true) {
        //playVoice(13);
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
      
     case 7:
        voice = soundMinim.loadFile(dataPath("m_i_am_not_sure.mp3"));
        voice.play();
        watchBeat = false;
      break;  
      
      //case 13:
      //  voice = soundMinim.loadFile(dataPath("m_i_am_not_sure.mp3"));
      //  voice.play();
      //  watchBeat = false;
      //break; 
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
      //processTripps();
      if ((channelselector == 1) | (channelselector == 2) | (channelselector == 8) | (channelselector == 14) ) processTripps();
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
///////////////////////////////////////////////////////////////////////////////
//
//    W_networking.pde (Networking Widget)
//
//    This widget provides networking capabilities in the OpenBCI GUI.
//    The networking protocols can be used for outputting data
//    from the OpenBCI GUI to any program that can receive UDP, OSC,
//    or LSL input, such as Matlab, MaxMSP, Python, C/C++, etc.
//
//    The protocols included are: UDP, OSC, and LSL.
//
//
//    Created by: Gabriel Ibagon (github.com/gabrielibagon), January 2017
//
///////////////////////////////////////////////////////////////////////////////


class W_networking extends Widget {

  /* Variables for protocol selection */
  int protocolIndex;
  String protocolMode;

  /* Widget CP5 */
  ControlP5 cp5_networking;
  ControlP5 cp5_networking_dropdowns;
  ControlP5 cp5_networking_baudRate;
  ControlP5 cp5_networking_portName;

  boolean dataDropdownsShouldBeClosed = false;
  // CColor dropdownColors_networking = new CColor();

  // PApplet ourApplet;

  /* UI Organization */
  /* Widget grid */
  int column0;
  int column1;
  int column2;
  int column3;
  int fullColumnWidth;
  int twoThirdsWidth;
  int row0;
  int row1;
  int row2;
  int row3;
  int row4;
  int row5;

  /* UI */
  Boolean osc_visible;
  Boolean udp_visible;
  Boolean lsl_visible;
  Boolean serial_visible;
  List<String> dataTypes;
  Button startButton;

  /* Networking */
  Boolean networkActive;

  /* Streams Objects */
  Stream stream1;
  Stream stream2;
  Stream stream3;

  List<String> baudRates;
  List<String> comPorts;
  String defaultBaud;

  W_networking(PApplet _parent){
    super(_parent);
    // ourApplet = _parent;

    networkActive = false;
    stream1 = null;
    stream2 = null;
    stream3 = null;

    dataTypes = Arrays.asList("None", "TimeSeries", "FFT", "EMG", "BandPower", "Focus", "Widget");
    defaultBaud = "115200";
    // baudRates = Arrays.asList("1200", "9600", "57600", "115200");
    baudRates = Arrays.asList("57600", "115200", "250000", "500000");
    protocolMode = "OSC"; //default to OSC
    addDropdown("Protocol", "Protocol", Arrays.asList("OSC", "UDP", "LSL", "Serial"), protocolIndex);
    comPorts = new ArrayList<String>(Arrays.asList(Serial.list()));
    println("comPorts = " + comPorts);


    initialize_UI();
    cp5_networking.setAutoDraw(false);
    cp5_networking_dropdowns.setAutoDraw(false);
    cp5_networking_portName.setAutoDraw(false);
    cp5_networking_baudRate.setAutoDraw(false);

  }

  /* ----- USER INTERFACE ----- */

  public void update(){
    super.update();
    if(protocolMode.equals("LSL")){
      if(stream1!=null){
        stream1.run();
      }
      if(stream2!=null){
        stream2.run();
      }
      if(stream2!=null){
        stream2.run();
      }
    }

    //put your code here...
    if(dataDropdownsShouldBeClosed){ //this if takes care of the scenario where you select the same widget that is active...
      dataDropdownsShouldBeClosed = false;
    } else {
      if(cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").isOpen()){
        if(!cp5_networking_dropdowns.getController("dataType1").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").close();
        }
      }
      if(!cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").isOpen()){
        if(cp5_networking_dropdowns.getController("dataType1").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").open();
        }
      }

      if(cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").isOpen()){
        if(!cp5_networking_dropdowns.getController("dataType2").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").close();
        }
      }
      if(!cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").isOpen()){
        if(cp5_networking_dropdowns.getController("dataType2").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").open();
        }
      }

      if(cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").isOpen()){
        if(!cp5_networking_dropdowns.getController("dataType3").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").close();
        }
      }
      if(!cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").isOpen()){
        if(cp5_networking_dropdowns.getController("dataType3").isMouseOver()){
          // println("2");
          cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").open();
        }
      }

      if(cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").isOpen()){
        if(!cp5_networking_baudRate.getController("baud_rate").isMouseOver()){
          // println("2");
          cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").close();
        }
      }
      if(!cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").isOpen() && !cp5_networking_dropdowns.getController("dataType1").isMouseOver()){
        if(cp5_networking_baudRate.getController("baud_rate").isMouseOver()){
          // println("2");
          cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").open();
        }
      }

      if(cp5_networking_portName.get(ScrollableList.class, "port_name").isOpen()){
        if(!cp5_networking_portName.getController("port_name").isMouseOver()){
          // println("2");
          cp5_networking_portName.get(ScrollableList.class, "port_name").close();
        }
      }
      if(!cp5_networking_portName.get(ScrollableList.class, "port_name").isOpen()  && !cp5_networking_dropdowns.getController("dataType1").isMouseOver() && !cp5_networking_baudRate.getController("baud_rate").isMouseOver()){
        if(cp5_networking_portName.getController("port_name").isMouseOver()){
          // println("2");
          cp5_networking_portName.get(ScrollableList.class, "port_name").open();
        }
      }
    }
  }

  public void draw(){
    super.draw();
    pushStyle();



    // fill(255,0,0);
    // rect(cp5_networking.getController("dataType1").getPosition()[0] - 1, cp5_networking.getController("dataType1").getPosition()[1] - 1, 100 + 2, cp5_networking.get(ScrollableList.class, "dataType1").getHeight()+2);

    showCP5();

    cp5_networking.draw();

    //draw dropdown strokes
    pushStyle();
    fill(255);
    if(!protocolMode.equals("Serial")){
      rect(cp5_networking_dropdowns.getController("dataType1").getPosition()[0] - 1, cp5_networking_dropdowns.getController("dataType1").getPosition()[1] - 1, 100 + 2, cp5_networking_dropdowns.getController("dataType1").getHeight()+2);
      rect(cp5_networking_dropdowns.getController("dataType2").getPosition()[0] - 1, cp5_networking_dropdowns.getController("dataType2").getPosition()[1] - 1, 100 + 2, cp5_networking_dropdowns.getController("dataType2").getHeight()+2);
      rect(cp5_networking_dropdowns.getController("dataType3").getPosition()[0] - 1, cp5_networking_dropdowns.getController("dataType3").getPosition()[1] - 1, 100 + 2, cp5_networking_dropdowns.getController("dataType3").getHeight()+2);
      cp5_networking_dropdowns.draw();
    }
    if(protocolMode.equals("Serial")){
      rect(cp5_networking_portName.getController("port_name").getPosition()[0] - 1, cp5_networking_portName.getController("port_name").getPosition()[1] - 1, cp5_networking_portName.getController("port_name").getWidth() + 2, cp5_networking_portName.getController("port_name").getHeight()+2);
      cp5_networking_portName.draw();
      rect(cp5_networking_baudRate.getController("baud_rate").getPosition()[0] - 1, cp5_networking_baudRate.getController("baud_rate").getPosition()[1] - 1, cp5_networking_baudRate.getController("baud_rate").getWidth() + 2, cp5_networking_baudRate.getController("baud_rate").getHeight()+2);
      cp5_networking_baudRate.draw();
      rect(cp5_networking_dropdowns.getController("dataType1").getPosition()[0] - 1, cp5_networking_dropdowns.getController("dataType1").getPosition()[1] - 1, cp5_networking_dropdowns.getController("dataType1").getWidth() + 2, cp5_networking_dropdowns.getController("dataType1").getHeight()+2);
      cp5_networking_dropdowns.draw();
    }
    popStyle();


    // cp5_networking_dropdowns.draw();

    fill(0,0,0);// Background fill: white
    textFont(h1,20);

    if(!protocolMode.equals("Serial")){
      // text("Data Type", column0,row1);
      text(" Stream 1",column1,row0);
      text(" Stream 2",column2,row0);
      text(" Stream 3",column3,row0);
    } else{
      // text("Data Type", column0,row0+15);
    }

    text("Data Type", column0,row1);


    startButton.draw();

    // textAlign(RIGHT,TOP);

    if(protocolMode.equals("OSC")){
      textFont(f4,40);
      text("OSC", x+20,y+h/8+15);
      textFont(h1,20);
      text("IP", column0,row2);
      text("Port", column0,row3);
      text("Address",column0,row4);
      text("Filters",column0,row5);
    }else if (protocolMode.equals("UDP")){
      textFont(f4,40);
      text("UDP", x+20,y+h/8+15);
      textFont(h1,20);
      text("IP", column0,row2);
      text("Port", column0,row3);
      text("Filters",column0,row4);
    }else if (protocolMode.equals("LSL")){
      textFont(f4,40);
      text("LSL", x+20,y+h/8+15);
      textFont(h1,20);
      text("Name", column0,row2);
      text("Type", column0,row3);
      text("# Chan", column0, row4);
    }else if (protocolMode.equals("Serial")){
      textFont(f4,40);
      text("Serial", x+20,y+h/8+15);
      textFont(h1,20);
      text("Baud/Port", column0,row2);
      // text("Port Name", column0,row3);
      text("Filters",column0,row3);
    }
    popStyle();

  }

  public void initialize_UI(){
    cp5_networking = new ControlP5(pApplet);
    cp5_networking_dropdowns = new ControlP5(pApplet);
    cp5_networking_baudRate = new ControlP5(pApplet);
    cp5_networking_portName = new ControlP5(pApplet);

    /* Textfields */
    // OSC
    createTextFields("osc_ip1","127.0.0.1");
    createTextFields("osc_port1","12345");
    createTextFields("osc_address1","/openbci");
    createTextFields("osc_ip2","127.0.0.1");
    createTextFields("osc_port2","12346");
    createTextFields("osc_address2","/openbci");
    createTextFields("osc_ip3","127.0.0.1");
    createTextFields("osc_port3","12347");
    createTextFields("osc_address3","/openbci");
    // UDP
    createTextFields("udp_ip1","127.0.0.1");
    createTextFields("udp_port1","12345");
    createTextFields("udp_ip2","127.0.0.1");
    createTextFields("udp_port2","12346");
    createTextFields("udp_ip3","127.0.0.1");
    createTextFields("udp_port3","12347");
    // LSL
    createTextFields("lsl_name1","obci_eeg1");
    createTextFields("lsl_type1","EEG");
    createTextFields("lsl_numchan1",Integer.toString(nchan));
    createTextFields("lsl_name2","obci_eeg2");
    createTextFields("lsl_type2","EEG");
    createTextFields("lsl_numchan2",Integer.toString(nchan));
    createTextFields("lsl_name3","obci_eeg3");
    createTextFields("lsl_type3","EEG");
    createTextFields("lsl_numchan3",Integer.toString(nchan));

    // Serial
    //grab list of existing serial port options and store into Arrays.list...



    createPortDropdown("port_name", comPorts);
    createBaudDropdown("baud_rate", baudRates);
    /* General Elements */

    createRadioButtons("filter1");
    createRadioButtons("filter2");
    createRadioButtons("filter3");

    createDropdown("dataType1", dataTypes);
    createDropdown("dataType2", dataTypes);
    createDropdown("dataType3", dataTypes);

    // Start Button
    startButton = new Button(x + w/2 - 70,y+h-40,200,20,"Start",14);
    startButton.setFont(p4,14);
    startButton.setColorNotPressed(color(184,220,105));
  }

  /* Shows and Hides appropriate CP5 elements within widget */
  public void showCP5(){



    osc_visible=false;
    udp_visible=false;
    lsl_visible=false;
    serial_visible=false;

    if(protocolMode.equals("OSC")){
      osc_visible = true;
    }else if (protocolMode.equals("UDP")){
      udp_visible = true;
    }else if (protocolMode.equals("LSL")){
      lsl_visible = true;
    }else if (protocolMode.equals("Serial")){
      serial_visible = true;
    }
    cp5_networking.get(Textfield.class, "osc_ip1").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_port1").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_address1").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_ip2").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_port2").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_address2").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_ip3").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_port3").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "osc_address3").setVisible(osc_visible);
    cp5_networking.get(Textfield.class, "udp_ip1").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "udp_port1").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "udp_ip2").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "udp_port2").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "udp_ip3").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "udp_port3").setVisible(udp_visible);
    cp5_networking.get(Textfield.class, "lsl_name1").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_type1").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_numchan1").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_name2").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_type2").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_numchan2").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_name3").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_type3").setVisible(lsl_visible);
    cp5_networking.get(Textfield.class, "lsl_numchan3").setVisible(lsl_visible);

    cp5_networking_portName.get(ScrollableList.class, "port_name").setVisible(serial_visible);
    cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").setVisible(serial_visible);

    cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").setVisible(true);

    if(!serial_visible){
      cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").setVisible(true);
      cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").setVisible(true);
    } else{
      cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").setVisible(false);
      cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").setVisible(false);
    }

    cp5_networking.get(RadioButton.class, "filter1").setVisible(true);

    if(!serial_visible){
      cp5_networking.get(RadioButton.class, "filter2").setVisible(true);
      cp5_networking.get(RadioButton.class, "filter3").setVisible(true);
    } else {
      cp5_networking.get(RadioButton.class, "filter2").setVisible(false);
      cp5_networking.get(RadioButton.class, "filter3").setVisible(false);
    }

  }

  /* Create textfields for network parameters */
  public void createTextFields(String name, String default_text){
    cp5_networking.addTextfield(name)
      .align(10,100,10,100)                   // Alignment
      .setSize(100,20)                         // Size of textfield
      .setFont(f2)
      .setFocus(false)                        // Deselects textfield
      .setColor(color(26,26,26))
      .setColorBackground(color(255,255,255)) // text field bg color
      .setColorValueLabel(color(0,0,0))       // text color
      .setColorForeground(color(26,26,26))    // border color when not selected
      .setColorActive(isSelected_color)       // border color when selected
      .setColorCursor(color(26,26,26))
      .setText(default_text)                  // Default text in the field
      .setCaptionLabel("")                    // Remove caption label
      .setVisible(false)                      // Initially hidden
      .setAutoClear(true)                     // Autoclear
      ;
  }

  /* Create radio buttons for filter toggling */
  public void createRadioButtons(String name){
    String id = name.substring(name.length()-1);
    cp5_networking.addRadioButton(name)
        .setSize(10,10)
        .setColorForeground(color(120))
        .setColorBackground(color(200,200,200)) // text field bg color
        .setColorActive(color(184,220,105))
        .setColorLabel(color(0))
        .setItemsPerRow(2)
        .setSpacingColumn(40)
        .addItem(id + "-Off", 0)
        .addItem(id + "-On", 1)
        // .addItem("Off",0)
        // .addItem("On",1)
        .activate(0)
        .setVisible(false)
        ;
  }

  /* Creating DataType Dropdowns */
  public void createDropdown(String name, List<String> _items){

    cp5_networking_dropdowns.addScrollableList(name)
        .setOpen(false)

        .setColorBackground(color(31,69,110)) // text field bg color
        .setColorValueLabel(color(255))       // text color
        .setColorCaptionLabel(color(255))
        .setColorForeground(color(125))    // border color when not selected
        .setColorActive(color(150, 170, 200))       // border color when selected
        // .setColorCursor(color(26,26,26))

        .setSize(100,(_items.size()+1)*(navH-4))// + maxFreqList.size())
        .setBarHeight(navH-4) //height of top/primary bar
        .setItemHeight(navH-4) //height of all item/dropdown bars
        .addItems(_items) // used to be .addItems(maxFreqList)
        .setVisible(false)
        ;
    cp5_networking_dropdowns.getController(name)
      .getCaptionLabel() //the caption label is the text object in the primary bar
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText("None")
      .setFont(h4)
      .setSize(14)
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(4)
      ;
    cp5_networking_dropdowns.getController(name)
      .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText("None")
      .setFont(h5)
      .setSize(12) //set the font size of the item bars to 14pt
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(3) //4-pixel vertical offset to center text
      ;
  }

  public void createBaudDropdown(String name, List<String> _items){
    cp5_networking_baudRate.addScrollableList(name)
        .setOpen(false)

        .setColorBackground(color(31,69,110)) // text field bg color
        .setColorValueLabel(color(255))       // text color
        .setColorCaptionLabel(color(255))
        .setColorForeground(color(125))    // border color when not selected
        .setColorActive(color(150, 170, 200))       // border color when selected
        // .setColorCursor(color(26,26,26))

        .setSize(100,(_items.size()+1)*(navH-4))// + maxFreqList.size())
        .setBarHeight(navH-4) //height of top/primary bar
        .setItemHeight(navH-4) //height of all item/dropdown bars
        .addItems(_items) // used to be .addItems(maxFreqList)
        .setVisible(false)
        ;
    cp5_networking_baudRate.getController(name)
      .getCaptionLabel() //the caption label is the text object in the primary bar
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText(defaultBaud)
      .setFont(h4)
      .setSize(14)
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(4)
      ;
    cp5_networking_baudRate.getController(name)
      .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText("None")
      .setFont(h5)
      .setSize(12) //set the font size of the item bars to 14pt
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(3) //4-pixel vertical offset to center text
      ;
  }

  public void createPortDropdown(String name, List<String> _items){
    cp5_networking_portName.addScrollableList(name)
        .setOpen(false)

        .setColorBackground(color(31,69,110)) // text field bg color
        .setColorValueLabel(color(255))       // text color
        .setColorCaptionLabel(color(255))
        .setColorForeground(color(125))    // border color when not selected
        .setColorActive(color(150, 170, 200))       // border color when selected
        // .setColorCursor(color(26,26,26))

        .setSize(100,(_items.size()+1)*(navH-4))// + maxFreqList.size())
        .setBarHeight(navH-4) //height of top/primary bar
        .setItemHeight(navH-4) //height of all item/dropdown bars
        .addItems(_items) // used to be .addItems(maxFreqList)
        .setVisible(false)
        ;
    cp5_networking_portName.getController(name)
      .getCaptionLabel() //the caption label is the text object in the primary bar
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText("None")
      .setFont(h4)
      .setSize(14)
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(4)
      ;
    cp5_networking_portName.getController(name)
      .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
      .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
      .setText("None")
      .setFont(h5)
      .setSize(12) //set the font size of the item bars to 14pt
      .getStyle() //need to grab style before affecting the paddingTop
      .setPaddingTop(3) //4-pixel vertical offset to center text
      ;
  }

  public void screenResized(){
    super.screenResized();

    cp5_networking.setGraphics(pApplet, 0,0);
    cp5_networking_dropdowns.setGraphics(pApplet, 0,0);
    cp5_networking_baudRate.setGraphics(pApplet, 0,0);
    cp5_networking_portName.setGraphics(pApplet, 0,0);

    column0 = x+w/20;
    // column1 = x+3*w/10;
    // column2 = x+5*w/10;
    // column3 = x+7*w/10;

    column1 = x+12*w/40;
    column2 = x+21*w/40;
    column3 = x+30*w/40;

    twoThirdsWidth = (column2+100) - column1;
    fullColumnWidth = (column3+100) - column1;

    row0 = y+h/4+10;
    row1 = y+4*h/10;
    row2 = y+5*h/10;
    row3 = y+6*h/10;
    row4 = y+7*h/10;
    row5 = y+8*h/10;
    int offset = 17;

    startButton.setPos(x + w/2 - 70, y + h - 40 );
    cp5_networking.get(Textfield.class, "osc_ip1").setPosition(column1, row2 - offset);
    cp5_networking.get(Textfield.class, "osc_port1").setPosition(column1, row3 - offset);
    cp5_networking.get(Textfield.class, "osc_address1").setPosition(column1, row4 - offset);
    cp5_networking.get(Textfield.class, "osc_ip2").setPosition(column2, row2 - offset);
    cp5_networking.get(Textfield.class, "osc_port2").setPosition(column2, row3 - offset);
    cp5_networking.get(Textfield.class, "osc_address2").setPosition(column2, row4 - offset);
    cp5_networking.get(Textfield.class, "osc_ip3").setPosition(column3, row2 - offset);
    cp5_networking.get(Textfield.class, "osc_port3").setPosition(column3, row3 - offset);
    cp5_networking.get(Textfield.class, "osc_address3").setPosition(column3, row4 - offset);
    cp5_networking.get(Textfield.class, "udp_ip1").setPosition(column1, row2 - offset);
    cp5_networking.get(Textfield.class, "udp_port1").setPosition(column1, row3 - offset);
    cp5_networking.get(Textfield.class, "udp_ip2").setPosition(column2, row2 - offset);
    cp5_networking.get(Textfield.class, "udp_port2").setPosition(column2, row3 - offset);
    cp5_networking.get(Textfield.class, "udp_ip3").setPosition(column3, row2 - offset);
    cp5_networking.get(Textfield.class, "udp_port3").setPosition(column3, row3 - offset);
    cp5_networking.get(Textfield.class, "lsl_name1").setPosition(column1,row2 - offset);
    cp5_networking.get(Textfield.class, "lsl_type1").setPosition(column1,row3 - offset);
    cp5_networking.get(Textfield.class, "lsl_numchan1").setPosition(column1,row4 - offset);
    cp5_networking.get(Textfield.class, "lsl_name2").setPosition(column2,row2 - offset);
    cp5_networking.get(Textfield.class, "lsl_type2").setPosition(column2,row3 - offset);
    cp5_networking.get(Textfield.class, "lsl_numchan2").setPosition(column2,row4 - offset);
    cp5_networking.get(Textfield.class, "lsl_name3").setPosition(column3,row2 - offset);
    cp5_networking.get(Textfield.class, "lsl_type3").setPosition(column3,row3 - offset);
    cp5_networking.get(Textfield.class, "lsl_numchan3").setPosition(column3,row4 - offset);


    if (protocolMode.equals("OSC") || protocolMode.equals("LSL")){
      cp5_networking.get(RadioButton.class, "filter1").setPosition(column1, row5 - 10);
      cp5_networking.get(RadioButton.class, "filter2").setPosition(column2, row5 - 10);
      cp5_networking.get(RadioButton.class, "filter3").setPosition(column3, row5 - 10);
    } else if (protocolMode.equals("UDP")){
      cp5_networking.get(RadioButton.class, "filter1").setPosition(column1, row4 - 10);
      cp5_networking.get(RadioButton.class, "filter2").setPosition(column2, row4 - 10);
      cp5_networking.get(RadioButton.class, "filter3").setPosition(column3, row4 - 10);
    } else if (protocolMode.equals("Serial")){
      cp5_networking.get(RadioButton.class, "filter1").setPosition(column1, row3 - 10);
      cp5_networking.get(RadioButton.class, "filter2").setPosition(column2, row3 - 10);
      cp5_networking.get(RadioButton.class, "filter3").setPosition(column3, row3 - 10);
    }

    //Serial Specific
    cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").setPosition(column1, row2-offset);
    // cp5_networking_portName.get(ScrollableList.class, "port_name").setPosition(column1, row3-offset);
    cp5_networking_portName.get(ScrollableList.class, "port_name").setPosition(column2, row2-offset);
    cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").setSize(100, (baudRates.size()+1)*(navH-4));
    // cp5_networking_portName.get(ScrollableList.class, "port_name").setSize(fullColumnWidth, (comPorts.size()+1)*(navH-4));
    // cp5_networking_portName.get(ScrollableList.class, "port_name").setSize(fullColumnWidth, (4)*(navH-4)); //
    cp5_networking_portName.get(ScrollableList.class, "port_name").setSize(twoThirdsWidth, (5)*(navH-4)); //twoThirdsWidth

    cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").setPosition(column1, row1-offset);
    cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").setPosition(column2, row1-offset);
    cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").setPosition(column3, row1-offset);
  }

  public void mousePressed(){
    super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
    if(startButton.isMouseHere()){
      startButton.setIsActive(true);
    }


  }

  public void mouseReleased(){
    super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    /* If start button was pressed */
    if(startButton.isActive && startButton.isMouseHere()){
      if(!networkActive){
        turnOnButton();         // Change appearance of button
        initializeStreams();    // Establish stream
        startNetwork();         // Begin streaming
      }else{
        turnOffButton();        // Change apppearance of button
        stopNetwork();          // Stop streams
      }
    }
    startButton.setIsActive(false);
  }

  /* Function call to hide all widget CP5 elements */
  public void hideElements(){
    cp5_networking.get(Textfield.class, "osc_ip1").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_port1").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_address1").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_ip2").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_port2").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_address2").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_ip3").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_port3").setVisible(false);
    cp5_networking.get(Textfield.class, "osc_address3").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_ip1").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_port1").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_ip2").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_port2").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_ip3").setVisible(false);
    cp5_networking.get(Textfield.class, "udp_port3").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_name1").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_type1").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_numchan1").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_name2").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_type2").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_numchan2").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_name3").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_type3").setVisible(false);
    cp5_networking.get(Textfield.class, "lsl_numchan3").setVisible(false);

    cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").setVisible(false);
    cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").setVisible(false);
    cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").setVisible(false);
    cp5_networking_portName.get(ScrollableList.class, "port_name").setVisible(false);
    cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").setVisible(false);

    cp5_networking.get(RadioButton.class, "filter1").setVisible(false);
    cp5_networking.get(RadioButton.class, "filter2").setVisible(false);
    cp5_networking.get(RadioButton.class, "filter3").setVisible(false);
    //%%%%%

  }

  /* Change appearance of Button to off */
  public void turnOffButton(){
    startButton.setColorNotPressed(color(184,220,105));
    startButton.setString("Start");
  }

  public void turnOnButton(){
    startButton.setColorNotPressed(color(224, 56, 45));
    startButton.setString("Stop");
  }

  /* Call to shutdown some UI stuff. Called from W_manager, maybe do this differently.. */
  public void shutDown(){
    hideElements();
    turnOffButton();
  }

  public void initializeStreams(){
    String ip;
    int port;
    String address;
    int filt_pos;
    String name;
    int nChanLSL;
    int baudRate;
    String type;
    String dt1="None";
    String dt2="None";
    String dt3="None";
    networkActive = true;
    switch ((int)cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").getValue()){
      case 0 : dt1 = "None";
        break;
      case 1 : dt1 = "TimeSeries";
        break;
      case 2 : dt1 = "FFT";
        break;
      case 3 : dt1 = "EMG";
        break;
      case 4 : dt1 = "BandPower";
        break;
      case 5 : dt1 = "Focus";
        break;
      case 6 : dt1 = "Widget";
        break;
    }
    switch ((int)cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").getValue()){
      case 0 : dt2 = "None";
        break;
      case 1 : dt2 = "TimeSeries";
        break;
      case 2 : dt2 = "FFT";
        break;
      case 3 : dt2 = "EMG";
        break;
      case 4 : dt2 = "BandPower";
        break;
      case 5 : dt2 = "Focus";
        break;
      case 6 : dt2 = "Widget";
        break;
    }
    switch ((int)cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").getValue()){
      case 0 : dt3 = "None";
        break;
      case 1 : dt3 = "TimeSeries";
        break;
      case 2 : dt3 = "FFT";
        break;
      case 3 : dt3 = "EMG";
        break;
      case 4 : dt3 = "BandPower";
        break;
      case 5 : dt3 = "Focus";
        break;
      case 6 : dt3 = "Widget";
        break;
    }

    // Establish OSC Streams
    if (protocolMode.equals("OSC")){
      if(!dt1.equals("None")){
        ip = cp5_networking.get(Textfield.class, "osc_ip1").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "osc_port1").getText());
        address = cp5_networking.get(Textfield.class, "osc_address1").getText();
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter1").getValue();
        stream1 = new Stream(dt1, ip, port, address, filt_pos, nchan);
      }else{
        stream1 = null;
      }
      if(!dt2.equals("None")){
        ip = cp5_networking.get(Textfield.class, "osc_ip2").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "osc_port2").getText());
        address = cp5_networking.get(Textfield.class, "osc_address2").getText();
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter2").getValue();
        stream2 = new Stream(dt2, ip, port, address, filt_pos, nchan);
      }else{
        stream2 = null;
      }
      if(!dt3.equals("None")){
        ip = cp5_networking.get(Textfield.class, "osc_ip3").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "osc_port3").getText());
        address = cp5_networking.get(Textfield.class, "osc_address3").getText();
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter3").getValue();
        stream3 = new Stream(dt3, ip, port, address, filt_pos, nchan);
      }else{
        stream3 = null;
      }

      // Establish UDP Streams
    }else if (protocolMode.equals("UDP")){
      if(!dt1.equals("None")){
        ip = cp5_networking.get(Textfield.class, "udp_ip1").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "udp_port1").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter1").getValue();
        stream1 = new Stream(dt1, ip, port, filt_pos, nchan);
      }else{
        stream1 = null;
      }
      if(!dt2.equals("None")){
        ip = cp5_networking.get(Textfield.class, "udp_ip2").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "udp_port2").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter2").getValue();
        stream2 = new Stream(dt2, ip, port, filt_pos, nchan);
      }else{
        stream2 = null;
      }
      if(!dt3.equals("None")){
        ip = cp5_networking.get(Textfield.class, "udp_ip3").getText();
        port = Integer.parseInt(cp5_networking.get(Textfield.class, "udp_port3").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter3").getValue();
        stream3 = new Stream(dt3, ip, port, filt_pos, nchan);
      }else{
        stream3 = null;
      }

      // Establish LSL Streams
    }else if (protocolMode.equals("LSL")){
      if(!dt1.equals("None")){
        name = cp5_networking.get(Textfield.class, "lsl_name1").getText();
        type = cp5_networking.get(Textfield.class, "lsl_type1").getText();
        nChanLSL = Integer.parseInt(cp5_networking.get(Textfield.class, "lsl_numchan1").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter1").getValue();
        stream1 = new Stream(dt1, name, type, nChanLSL, filt_pos, nchan);
      }else{
        stream1 = null;
      }
      if(!dt2.equals("None")){
        name = cp5_networking.get(Textfield.class, "lsl_name2").getText();
        type = cp5_networking.get(Textfield.class, "lsl_type2").getText();
        nChanLSL = Integer.parseInt(cp5_networking.get(Textfield.class, "lsl_numchan2").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter2").getValue();
        stream2 = new Stream(dt2, name, type, nChanLSL, filt_pos, nchan);
      }else{
        stream2 = null;
      }
      if(!dt3.equals("None")){
        name = cp5_networking.get(Textfield.class, "lsl_name3").getText();
        type = cp5_networking.get(Textfield.class, "lsl_type3").getText();
        nChanLSL = Integer.parseInt(cp5_networking.get(Textfield.class, "lsl_numchan3").getText());
        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter3").getValue();
        stream3 = new Stream(dt3, name, type, nChanLSL, filt_pos, nchan);
      }else{
        stream3 = null;
      }
    } else if (protocolMode.equals("Serial")){
      // %%%%%
      if(!dt1.equals("None")){
        println(comPorts.get((int)(cp5_networking_portName.get(ScrollableList.class, "port_name").getValue())));
        name = comPorts.get((int)(cp5_networking_portName.get(ScrollableList.class, "port_name").getValue()));
        // name = cp5_networking_portName.get(ScrollableList.class, "port_name").getItem((int)cp5_networking_portName.get(ScrollableList.class, "port_name").getValue());
        println(Integer.parseInt(baudRates.get((int)(cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").getValue()))));
        baudRate = Integer.parseInt(baudRates.get((int)(cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").getValue())));

        filt_pos = (int)cp5_networking.get(RadioButton.class, "filter1").getValue();
        stream1 = new Stream(dt1, name, baudRate, filt_pos, pApplet, nchan);  //String dataType, String portName, int baudRate, int filter, PApplet _this
      }else{
        stream1 = null;
      }
    }
  }

  /* Start networking */
  public void startNetwork(){
    if(stream1!=null){
      stream1.start();
    }
    if(stream2!=null){
      stream2.start();
    }
    if(stream3!=null){
      stream3.start();
    }
  }

  /* Stop networking */
  public void stopNetwork(){
    networkActive = false;

    if (stream1!=null){
      stream1.quit();
      stream1=null;
    }
    if (stream2!=null){
      stream2.quit();
      stream2=null;
    }
    if (stream3!=null){
      stream3.quit();
      stream3=null;
    }
  }

  public void clearCP5(){
    //clears all controllers from ControlP5 instance...
    w_networking.cp5_networking.dispose();
    w_networking.cp5_networking_dropdowns.dispose();
    println("clearing cp5_networking...");
  }

  public void closeAllDropdowns(){
    dataDropdownsShouldBeClosed = true;
    w_networking.cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").close();
    w_networking.cp5_networking_dropdowns.get(ScrollableList.class, "dataType2").close();
    w_networking.cp5_networking_dropdowns.get(ScrollableList.class, "dataType3").close();
    w_networking.cp5_networking_baudRate.get(ScrollableList.class, "baud_rate").close();
    w_networking.cp5_networking_portName.get(ScrollableList.class, "port_name").close();
  }
};

class Stream extends Thread{
  String protocol;
  String dataType;
  String ip;
  int port;
  String address;
  int filter;
  String streamType;
  String streamName;
  int nChanLSL;
  int numChan = 0;

  Boolean isStreaming;
  Boolean newData = false;
  // Data buffers
  int start = dataBuffY_filtY_uV[0].length-11;
  int end = dataBuffY_filtY_uV[0].length-1;
  int bufferLen = end-start;
  float[] dataToSend = new float[numChan*bufferLen];

  //OSC Objects
  OscP5 osc;
  NetAddress netaddress;
  OscMessage msg;
  //UDP Objects
  UDP udp;
  ByteBuffer buffer;
  // LSL objects
  LSL.StreamInfo info_data;
  LSL.StreamOutlet outlet_data;
  LSL.StreamInfo info_aux;
  LSL.StreamOutlet outlet_aux;

  // Serial objects %%%%%
  Serial serial_networking;
  String portName;
  int baudRate;
  String serialMessage = "";

  PApplet pApplet;

  private void updateNumChan(int _numChan) {
    numChan = _numChan;
    println("Stream update numChan to " + numChan);
    dataToSend = new float[numChan * nPointsPerUpdate];
    println("nPointsPerUpdate " + nPointsPerUpdate);

    println("dataToSend len: " + numChan * nPointsPerUpdate);
  }

  /* OSC Stream */
  Stream(String dataType, String ip, int port, String address, int filter, int _nchan){
    this.protocol = "OSC";
    this.dataType = dataType;
    this.ip = ip;
    this.port = port;
    this.address = address;
    this.filter = filter;
    this.isStreaming = false;
    updateNumChan(_nchan);
    try{
      closeNetwork(); //make sure everything is closed!
    }catch (Exception e){
    }
  }
  /*UDP Stream */
  Stream(String dataType, String ip, int port, int filter, int _nchan){
    this.protocol = "UDP";
    this.dataType = dataType;
    this.ip = ip;
    this.port = port;
    this.filter = filter;
    this.isStreaming = false;
    updateNumChan(_nchan);
    if(this.dataType.equals("TimeSeries")){
      buffer = ByteBuffer.allocate(4*numChan);
    }else{
      buffer = ByteBuffer.allocate(4*126);
    }
    try{
      closeNetwork(); //make sure everything is closed!
    }catch (Exception e){
    }
  }
  /* LSL Stream */
  Stream(String dataType, String streamName, String streamType, int nChanLSL, int filter, int _nchan){
    this.protocol = "LSL";
    this.dataType = dataType;
    this.streamName = streamName;
    this.streamType = streamType;
    this.nChanLSL = nChanLSL;
    this.filter = filter;
    this.isStreaming = false;
    updateNumChan(_nchan);
    try{
      closeNetwork(); //make sure everything is closed!
    }catch (Exception e){
    }
  }

  // Serial Stream %%%%%
  Stream(String dataType, String portName, int baudRate, int filter, PApplet _this, int _nchan){
    // %%%%%
    this.protocol = "Serial";
    this.dataType = dataType;
    this.portName = portName;
    this.baudRate = baudRate;
    this.filter = filter;
    this.isStreaming = false;
    this.pApplet = _this;
    updateNumChan(_nchan);
    if(this.dataType.equals("TimeSeries")){
      buffer = ByteBuffer.allocate(4*numChan);
    }else{
      buffer = ByteBuffer.allocate(4*126);
    }

    try{
      closeNetwork();
    }catch(Exception e){
      //nothing
    }
  }

  public void start(){
    this.isStreaming = true;
    if(!this.protocol.equals("LSL")){
      super.start();
    }else{
      openNetwork();
    }
  }

  public void run(){
    if (!this.protocol.equals("LSL")){
      openNetwork();
      while(this.isStreaming){
        if(!isRunning){
          try{
            Thread.sleep(1);
          }catch (InterruptedException e){
            println(e);
          }
        }else{
            if (checkForData()){
              if (this.dataType.equals("TimeSeries")){
                sendTimeSeriesData();
              }else if (this.dataType.equals("FFT")){
                sendFFTData();
              }else if (this.dataType.equals("EMG")){
                sendEMGData();
              }else if (this.dataType.equals("BandPower")){
                sendPowerBandData();
              }else if (this.dataType.equals("Focus")){
                sendFocusData();
              }else if (this.dataType.equals("WIDGET")){
                sendWidgetData();
              }
              setDataFalse();
            }else{
              try{
                Thread.sleep(1);
              }catch (InterruptedException e){
                println(e);
              }
            }
          }
        }
    }else if (this.protocol.equals("LSL")){
      if (!isRunning){
        try{
          Thread.sleep(1);
        }catch (InterruptedException e){
          println(e);
        }
      }else{
        if (checkForData()){
          if (this.dataType.equals("TimeSeries")){
            sendTimeSeriesData();
          }else if (this.dataType.equals("FFT")){
            sendFFTData();
          }else if (this.dataType.equals("EMG")){
            sendEMGData();
          }else if (this.dataType.equals("BandPower")){
            sendPowerBandData();
          }else if (this.dataType.equals("Focus")){
            sendFocusData();
          }else if (this.dataType.equals("WIDGET")){
            sendWidgetData();
          }
          setDataFalse();
          // newData = false;
        }
      }
    }
  }

  public Boolean checkForData(){
    if(this.dataType.equals("TimeSeries")){
      return dataProcessing.newDataToSend;
    }else if (this.dataType.equals("FFT")){
      return dataProcessing.newDataToSend;
    }else if (this.dataType.equals("EMG")){
      return dataProcessing.newDataToSend;
    }else if (this.dataType.equals("BandPower")){
      return dataProcessing.newDataToSend;
    }else if (this.dataType.equals("Focus")){
      return dataProcessing.newDataToSend;
    }else if (this.dataType.equals("WIDGET")){
      /* ENTER YOUR WIDGET "NEW DATA" RETURN FUNCTION */
    }
    return false;
  }

  public void setDataFalse(){
    if(this.dataType.equals("TimeSeries")){
      dataProcessing.newDataToSend = false;
    }else if (this.dataType.equals("FFT")){
      dataProcessing.newDataToSend = false;
    }else if (this.dataType.equals("EMG")){
      dataProcessing.newDataToSend = false;
    }else if (this.dataType.equals("BandPower")){
      dataProcessing.newDataToSend = false;
    }else if (this.dataType.equals("Focus")){
      dataProcessing.newDataToSend = false;
    }else if (this.dataType.equals("WIDGET")){
      /* ENTER YOUR WIDGET "NEW DATA" RETURN FUNCTION */
    }
  }
  /* This method contains all of the policies for sending data types */
  public void sendTimeSeriesData(){
    // TIME SERIES UNFILTERED
    if(filter==0){
      // OSC
      if(this.protocol.equals("OSC")){
        for(int i=0;i<nPointsPerUpdate;i++){
          msg.clearArguments();
          for(int j=0;j<numChan;j++){
            msg.add(yLittleBuff_uV[j][i]);
          }
         try{
           this.osc.send(msg,this.netaddress);
         }catch (Exception e){
           println(e);
         }
       }
       // UDP
     }else if (this.protocol.equals("UDP")){
       for(int i=0;i<nPointsPerUpdate;i++){
         String outputter = "{\"type\":\"eeg\",\"data\":[";
         for (int j = 0; j < numChan; j++){
           outputter += str(yLittleBuff_uV[j][i]);
           if (j != numChan - 1) {
             outputter += ",";
           } else {
             outputter += "]}\r\n";
           }
         }
         try {
           this.udp.send(outputter, this.ip, this.port);
         } catch (Exception e) {
           println(e);
         }
       }
       // LSL
     } else if (this.protocol.equals("LSL")) {
       for (int i=0; i<nPointsPerUpdate;i++){
         for(int j=0;j<numChan;j++){
           dataToSend[j+numChan*i] = yLittleBuff_uV[j][i];
         }
       }
       outlet_data.push_chunk(dataToSend);
       // SERIAL
     }else if (this.protocol.equals("Serial")){         // Serial Output unfiltered
       for(int i=0;i<nPointsPerUpdate;i++){
         serialMessage = "["; //clear message
         for(int j=0;j<numChan;j++){
           float chan_uV = yLittleBuff_uV[j][i];//get chan uV float value and truncate to 3 decimal places
           String chan_uV_3dec = String.format("%.3f", chan_uV);
           serialMessage += chan_uV_3dec;//  serialMesage += //add 3 decimal float chan uV value as string to serialMessage
           if(j < numChan-1){
             serialMessage += ",";  //add a comma to serialMessage to separate chan values, as long as it isn't last value...
           }
         }
         serialMessage += "]";  //close the message w/ "]"
         try{
           //  println(serialMessage);
           this.serial_networking.write(serialMessage);          //write message to serial
         }catch (Exception e){
           println(e);
         }
       }
     }


     // TIME SERIES FILTERED
    }else if (filter==1){
      if (this.protocol.equals("OSC")){
        for(int i=0;i<nPointsPerUpdate;i++){
          msg.clearArguments();
          for(int j=0;j<numChan;j++){
            msg.add(dataBuffY_filtY_uV[j][start+i]);
          }
         try{
           this.osc.send(msg,this.netaddress);
         }catch (Exception e){
           println(e);
         }
       }
     } else if (this.protocol.equals("UDP")){
       for(int i=0;i<nPointsPerUpdate;i++){
         String outputter = "{\"type\":\"eeg\",\"data\":[";
         for (int j = 0; j < numChan; j++){
           outputter += str(dataBuffY_filtY_uV[j][start+i]);
           if (j != numChan - 1) {
             outputter += ",";
           } else {
             outputter += "]}\r\n";
           }
         }
         try {
           this.udp.send(outputter, this.ip, this.port);
         } catch (Exception e) {
           println(e);
         }
       }
     }else if (this.protocol.equals("LSL")){
       for (int i=0; i<nPointsPerUpdate;i++){
         for(int j=0;j<numChan;j++){
           dataToSend[j+numChan*i] = dataBuffY_filtY_uV[j][i];
         }
       }
       outlet_data.push_chunk(dataToSend);
     }else if (this.protocol.equals("Serial")){
       for(int i=0;i<nPointsPerUpdate;i++){
         serialMessage = "["; //clear message
         for(int j=0;j<numChan;j++){
           float chan_uV_filt = dataBuffY_filtY_uV[j][start+i];//get chan uV float value and truncate to 3 decimal places
           String chan_uV_filt_3dec = String.format("%.3f", chan_uV_filt);
           serialMessage += chan_uV_filt_3dec;//  serialMesage += //add 3 decimal float chan uV value as string to serialMessage
           if(j < numChan-1){
             serialMessage += ",";  //add a comma to serialMessage to separate chan values, as long as it isn't last value...
           }
         }
         serialMessage += "]";  //close the message w/ "]"
         try{
           //  println(serialMessage);
           this.serial_networking.write(serialMessage);          //write message to serial
         }catch (Exception e){
           println(e);
         }
       }
     }
   }
 }

  public void sendFFTData(){
   // UNFILTERED
   if(this.filter==0 || this.filter==1){
     // OSC
     if (this.protocol.equals("OSC")){
       for (int i=0;i<numChan;i++){
         msg.clearArguments();
         msg.add(i+1);
         for (int j=0;j<125;j++){
           msg.add(fftBuff[i].getBand(j));
         }
         try{
           this.osc.send(msg,this.netaddress);
         }catch (Exception e){
           println(e);
         }
       }
      // UDP
     }else if (this.protocol.equals("UDP")){
       String outputter = "{\"type\":\"fft\",\"data\":[[";
       for (int i = 0;i < numChan; i++){
         for (int j = 0; j < 125; j++) {
           outputter += str(fftBuff[i].getBand(j));
           if (j != 125 - 1) {
             outputter += ",";
           }
         }
         if (i != numChan - 1) {
           outputter += "],[";
         } else {
           outputter += "]]}\r\n";
         }
       }
       try {
         this.udp.send(outputter, this.ip, this.port);
       } catch (Exception e) {
         println(e);
       }
       // LSL
     }else if (this.protocol.equals("LSL")){
       /* */
      }else if (this.protocol.equals("Serial")){
        // Send FFT Data over Serial ... %%%%%
        // println("Sending FFT data over Serial...");
        for (int i=0;i<numChan;i++){
          serialMessage = "[" + (i+1) + ","; //clear message
          for (int j=0;j<125;j++){
            float fft_band = fftBuff[i].getBand(j);
            String fft_band_3dec = String.format("%.3f", fft_band);
            serialMessage += fft_band_3dec;
            if(j < 125-1){
              serialMessage += ",";  //add a comma to serialMessage to separate chan values, as long as it isn't last value...
            }
          }
          serialMessage += "]";
          try{
            // println(serialMessage);
            this.serial_networking.write(serialMessage);
          }catch (Exception e){
            println(e);
          }
        }
      }
    }
  }

  public void sendPowerBandData(){
    // UNFILTERED & FILTERED ... influenced globally by the FFT filters dropdown ... just like the FFT data
    int numBandPower = 5; //DELTA, THETA, ALPHA, BETA, GAMMA

    if(this.filter==0 || this.filter==1){
      // OSC
      if (this.protocol.equals("OSC")){
        for (int i=0;i<numChan;i++){
          msg.clearArguments();
          msg.add(i+1);
          for (int j=0;j<numBandPower;j++){
            msg.add(dataProcessing.avgPowerInBins[i][j]); // [CHAN][BAND]
          }
          try{
            this.osc.send(msg,this.netaddress);
          }catch (Exception e){
            println(e);
          }
        }
       // UDP
      }else if (this.protocol.equals("UDP")){
        // DELTA, THETA, ALPHA, BETA, GAMMA
        String outputter = "{\"type\":\"bandPower\",\"data\":[[";
        for (int i = 0;i < numChan; i++){
          for (int j=0;j<numBandPower;j++){
            outputter += str(dataProcessing.avgPowerInBins[i][j]); //[CHAN][BAND]
            if (j != numBandPower - 1) {
              outputter += ",";
            }
          }
          if (i != numChan - 1) {
            outputter += "],[";
          } else {
            outputter += "]]}\r\n";
          }
        }
        try {
          this.udp.send(outputter, this.ip, this.port);
        } catch (Exception e) {
          println(e);
        }
        // LSL
      }else if (this.protocol.equals("LSL")){

        float[] avgPowerLSL = new float[numChan*numBandPower];
        for (int i=0; i<numChan;i++){
           for(int j=0;j<numBandPower;j++){
             dataToSend[j+numChan*i] = dataProcessing.avgPowerInBins[i][j];
           }
         }
         outlet_data.push_chunk(dataToSend);
       }else if (this.protocol.equals("Serial")){
          for (int i=0;i<numChan;i++){
            serialMessage = "[" + (i+1) + ","; //clear message
            for (int j=0;j<numBandPower;j++){
              float power_band = dataProcessing.avgPowerInBins[i][j];
              String power_band_3dec = String.format("%.3f", power_band);
              serialMessage += power_band_3dec;
              if(j < numBandPower-1){
                serialMessage += ",";  //add a comma to serialMessage to separate chan values, as long as it isn't last value...
              }
            }
            serialMessage += "]";
            try{
              // println(serialMessage);
              this.serial_networking.write(serialMessage);
            }catch (Exception e){
              println(e);
            }
          }
       }
     }
  }

  public void sendEMGData(){
    // UNFILTERED & FILTERED ... influenced globally by the FFT filters dropdown ... just like the FFT data

    if(this.filter==0 || this.filter==1){
      // OSC
      if (this.protocol.equals("OSC")){
        for (int i=0;i<numChan;i++){
          msg.clearArguments();
          msg.add(i+1);
          //ADD NORMALIZED EMG CHANNEL DATA
          msg.add(w_emg.motorWidgets[i].output_normalized);
          // println(i + " | " + w_emg.motorWidgets[i].output_normalized);
          try{
            this.osc.send(msg,this.netaddress);
          }catch (Exception e){
            println(e);
          }
        }
       // UDP
      } else if (this.protocol.equals("UDP")) {
        String outputter = "{\"type\":\"emg\",\"data\":[";
        for (int i = 0;i < numChan; i++){
          outputter += str(w_emg.motorWidgets[i].output_normalized);
          if (i != numChan - 1) {
            outputter += ",";
          } else {
            outputter += "]}\r\n";
          }
        }
        try {
          this.udp.send(outputter, this.ip, this.port);
        } catch (Exception e) {
          println(e);
        }
        // LSL
      }else if (this.protocol.equals("LSL")){
        if(filter==0){
           for(int j=0;j<numChan;j++){
             dataToSend[j] = w_emg.motorWidgets[j].output_normalized;
           }
           outlet_data.push_sample(dataToSend);
         }
       }else if (this.protocol.equals("Serial")){     // Send NORMALIZED EMG CHANNEL Data over Serial ... %%%%%
         for (int i=0;i<numChan;i++){
            serialMessage = "[" + (i+1) + ","; //clear message
            float emg_normalized = w_emg.motorWidgets[i].output_normalized;
            String emg_normalized_3dec = String.format("%.3f", emg_normalized);
            serialMessage += emg_normalized_3dec + "]";
           try{
            //  println(serialMessage);
             this.serial_networking.write(serialMessage);
           }catch (Exception e){
             println(e);
           }
         }
       }
     }
  }


  public void sendFocusData(){
    // UNFILTERED & FILTERED ... influenced globally by the FFT filters dropdown ... just like the FFT data

    if(this.filter==0 || this.filter==1){
      // OSC
      if (this.protocol.equals("OSC")){
        msg.clearArguments();
        //ADD Focus Data
        msg.add(w_focus.isFocused);
        println(w_focus.isFocused);
        try{
          this.osc.send(msg,this.netaddress);
        }catch (Exception e){
          println(e);
        }
      // UDP
      }else if (this.protocol.equals("UDP")){
        String outputter = "{\"type\":\"focus\",\"data\":";
        outputter += str(w_focus.isFocused ? 1.0f : 0.0f);
        outputter += "]}\r\n";
        try {
          this.udp.send(outputter, this.ip, this.port);
        } catch (Exception e) {
          println(e);
        }
      // LSL
      }else if (this.protocol.equals("LSL")){
        // convert boolean to float and only sends the first data
        float temp = w_focus.isFocused ? 1.0f : 0.0f;
        dataToSend[0] = temp;
        outlet_data.push_chunk(dataToSend);
      // Serial
      }else if (this.protocol.equals("Serial")){     // Send NORMALIZED EMG CHANNEL Data over Serial ... %%%%%
        for (int i=0;i<numChan;i++){
          serialMessage = ""; //clear message
          String isFocused = Boolean.toString(w_focus.isFocused);
          serialMessage += isFocused;
          try{
            println(serialMessage);
            this.serial_networking.write(serialMessage);
          }catch (Exception e){
            println(e);
          }
        }
      }
    }
  }

  public void sendWidgetData(){
    /* INSERT YOUR CODE HERE */
  }

  public void quit(){
    this.isStreaming=false;
    closeNetwork();
    interrupt();
  }

  public void closeNetwork(){
    if (this.protocol.equals("OSC")){
      try{
        this.osc.stop();
      }catch(Exception e){
        println(e);
      }
    }else if (this.protocol.equals("UDP")){
        this.udp.close();
    }else if (this.protocol.equals("LSL")){
      outlet_data.close();
    }else if (this.protocol.equals("Serial")){
      //Close Serial Port %%%%%
      try{
        serial_networking.clear();
        serial_networking.stop();
        println("Successfully closed SERIAL/COM port " + this.portName);
      } catch(Exception e){
        println("Failed to close SERIAL/COM port " + this.portName);
      }
    }
  }

  public void openNetwork(){
    println(getAttributes());
    if(this.protocol.equals("OSC")){
      //Possibly enter a nice custom exception here
      this.osc = new OscP5(this,this.port + 1000);
      this.netaddress = new NetAddress(this.ip,this.port);
      this.msg = new OscMessage(this.address);
    }else if (this.protocol.equals("UDP")){
      this.udp = new UDP(this);
      this.udp.setBuffer(20000);
      this.udp.listen(false);
      this.udp.log(false);
      println("UDP successfully connected");
      output("UDP successfully connected");
    }else if (this.protocol.equals("LSL")){
      String stream_id = "openbcieeg12345";
      info_data = new LSL.StreamInfo(
                            this.streamName,
                            this.streamType,
                            this.nChanLSL,
                            getSampleRateSafe(),
                            LSL.ChannelFormat.float32,
                            stream_id
                          );
      outlet_data = new LSL.StreamOutlet(info_data);
    }else if (this.protocol.equals("Serial")){
      //Open Serial Port! %%%%%
      try{
        serial_networking = new Serial(this.pApplet, this.portName, this.baudRate);
        serial_networking.clear();
        verbosePrint("Successfully opened SERIAL/COM: " + this.portName);
        output("Successfully opened SERIAL/COM (" + this.baudRate + "): " + this.portName );
      }catch(Exception e){
        verbosePrint("W_networking.pde: could not open SERIAL PORT: " + this.portName);
        println("Error: " + e);
      }
    }
  }

  public List getAttributes(){
    List attributes = new ArrayList();
    if (this.protocol.equals("OSC")){
      attributes.add(this.dataType);
      attributes.add(this.ip);
      attributes.add(this.port);
      attributes.add(this.address);
      attributes.add(this.filter);
    }else if(this.protocol.equals("UDP")){
      attributes.add(this.dataType);
      attributes.add(this.ip);
      attributes.add(this.port);
      attributes.add(this.filter);
    }
    else if (this.protocol.equals("LSL")){
      attributes.add(this.dataType);
      attributes.add(this.streamName);
      attributes.add(this.streamType);
      attributes.add(this.nChanLSL);
      attributes.add(this.filter);
    }
    else if (this.protocol.equals("Serial")){
      // Add Serial Port Attributes %%%%%
    }
    return attributes;
  }
}

/* Dropdown Menu Callback Functions */
/**
 * @description Sets the selected protocol mode from the widget's dropdown menu
 * @param `n` {int} - Index of protocol item selected in menu
 */
public void Protocol(int protocolIndex){
  if (protocolIndex==0){
    w_networking.protocolMode = "OSC";
  }else if (protocolIndex==1){
    w_networking.protocolMode = "UDP";
  }else if (protocolIndex==2){
    w_networking.protocolMode = "LSL";
  }else if (protocolIndex==3){
    w_networking.protocolMode = "Serial";
  }
  println(w_networking.protocolMode + " selected from Protocol Menu");
  w_networking.screenResized();
  w_networking.showCP5();
  closeAllDropdowns();

}

public void dataType1(int n){
  w_networking.closeAllDropdowns();
}
public void dataType2(int n){
  w_networking.closeAllDropdowns();
}
public void dataType3(int n){
  w_networking.closeAllDropdowns();
}
public void port_name(int n){
  w_networking.closeAllDropdowns();
}
public void baud_rate(int n){
  w_networking.closeAllDropdowns();
}
class W_Trainer extends Widget {

    //to see all core variables/methods of the Widget class, refer to Widget.pde
    //put your custom variables here...

    Button trainButton;
    Button clearTrainButton;
    Button testButton;
    Button cycleModeButton;
    
    boolean train_positive = false;
    boolean testing = false;
    boolean display_testing = false;
    boolean showColor = false;
    
    float positive_training_alpha_avr = 0.0f;
    float positive_training_alpha_data = 0.0f;
    float negitive_training_alpha_avr = 0.0f;
    float negitive_training_alpha_data = 0.0f;
    
    float positive_training_beta_avr = 0.0f;
    float positive_training_beta_data = 0.0f;
    float negitive_training_beta_avr = 0.0f;
    float negitive_training_beta_data = 0.0f;
    
    float lookleft = 0.0f;
    float lookleft1 = 0.0f;
    
    
    

    int train_delay = 4000;
    int last_delay = 0;

    int positive_training_counter = 0;
    int negitive_training_counter = 0;
    int test_positive_counter = 0;


    int t1 = color(255, 0, 0);
    int t2 = color(0, 0, 0);

    int[] colors = {
        t1,
        t2
    };

    PApplet parent;



    W_Trainer(PApplet _parent) {
        super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        parent = _parent;




    }



    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        //put your code here...

        //processTripps();
    }

    public void draw() {


        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        pushStyle();
        noStroke();
        fill(0);
        rect(x, y, w, h);


        trainButton = new Button((int)(x) + 50, (int)(y) - navHeight + 2, 58, navHeight - 6, "Train", fontInfo.buttonLabel_size);
        if (training) {
            trainButton.setColorNotPressed(color(0, 255, 0));
        } else {
            trainButton.setColorNotPressed(color(57, 128, 204));
        }
        trainButton.textColorNotActive = color(0);
        trainButton.setCornerRoundess((int)(navHeight - 6));
        trainButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        trainButton.draw();
        
        
        clearTrainButton = new Button((int)(x) + 120, (int)(y) - navHeight + 2, 58, navHeight - 6, "Clear", fontInfo.buttonLabel_size);
        clearTrainButton.setColorNotPressed(color(57, 128, 204));
       
        clearTrainButton.textColorNotActive = color(0);
        clearTrainButton.setCornerRoundess((int)(navHeight - 6));
        clearTrainButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        clearTrainButton.draw();
        
        
        if (trainingMode) {
            cycleModeButton = new Button((int)(x) + (int)(w) / 2 - 10, (int)(y) - navHeight + 2, 58, navHeight - 6, "Positive", fontInfo.buttonLabel_size);
            cycleModeButton.setColorNotPressed(color(0, 255, 0));
        } else {
            cycleModeButton = new Button((int)(x) + (int)(w) / 2 - 10, (int)(y) - navHeight + 2, 58, navHeight - 6, "Negitive", fontInfo.buttonLabel_size);
            cycleModeButton.setColorNotPressed(color(57, 128, 204));
        }
        cycleModeButton.textColorNotActive = color(0);
        cycleModeButton.setCornerRoundess((int)(navHeight - 6));
        cycleModeButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        cycleModeButton.draw();

        testButton = new Button((int)(x) + (int)(w)  - 100, (int)(y) - navHeight + 2, 58, navHeight - 6, "Test", fontInfo.buttonLabel_size);
        if (testing) {
            testButton.setColorNotPressed(color(0, 255, 0));
        } else {
            testButton.setColorNotPressed(color(57, 128, 204));
        }
        testButton.textColorNotActive = color(0);
        testButton.setCornerRoundess((int)(navHeight - 6));
        testButton.setHelpText("Click this button to connect to Darwin Visual Matrix");
        testButton.draw();
        
        
        



        stroke(1, 18, 41, 125);
        
        if (training){
          if (trainingMode){
            train_positive();
          }
          else {
            train_negitive();
          }
        }
        
        if (testing){
          if (trainingMode){
            test_positive();
          }
          else {
            test_negitive();
          }
        }
        

      
        // float rx = x, ry = y + 2* navHeight, rw = w, rh = h - 2*navHeight;
        float rx = x, ry = y, rw = w, rh = h;
        float scaleFactor = 1.0f;
        float scaleFactorJaw = 1.5f;
        int rowNum = 4;


        int index = 0;
        float currx, curry;
        //new


        popStyle();

    }
    
    public void test_results(){
      float margin = .07f;
      float highMargin = 1 + margin;
      float lowMargin = 1 - margin;
      
      
      if ((dataProcessing.headWidePower[ALPHA] > positive_training_alpha_avr * lowMargin) && (dataProcessing.headWidePower[ALPHA] < positive_training_alpha_avr * highMargin) && 
          (dataProcessing.headWidePower[BETA] > positive_training_beta_avr * lowMargin) && (dataProcessing.headWidePower[BETA] < positive_training_beta_avr * highMargin)) {
        println("training alpha avr: " + positive_training_alpha_avr);
        println("current alpha reading: " + dataProcessing.headWidePower[ALPHA]);
        println("training beta avr: " + positive_training_alpha_avr);
        println("current beta reading: " + dataProcessing.headWidePower[BETA]);
        test_positive_counter +=1;
        
      }
      else{
        test_positive_counter = 0;
      }
      if (dataProcessing.headWidePower[ALPHA] < positive_training_alpha_avr * lowMargin) println("alpha too low ");
      if (dataProcessing.headWidePower[BETA] < positive_training_beta_avr * lowMargin) println("beta too low ");
      
      if (dataProcessing.headWidePower[ALPHA] > positive_training_alpha_avr * highMargin) println("alpha too high ");
      if (dataProcessing.headWidePower[BETA] > positive_training_beta_avr * highMargin) println("beta too high ");
      
      
      if (test_positive_counter >= 2){
        playVoice(0);
        test_positive_counter = 0;
      }
      
      if (dataProcessing.data_std_uV[0] > lookleft * .95f && dataProcessing.data_std_uV[0] < lookleft * 1.05f) println("looking Left");
         
      
    }
    
    public void test_negitive(){
      fill(t2);
      noStroke();
      rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
      stroke(126);
      line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
      line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
            
      test_results();
        
    }
    
    public void test_positive(){
      if (millis() - train_delay > last_delay) {
          showColor = !showColor;
          last_delay = millis();
      }
      if (showColor) {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
      } else {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          //marker_indicater = 0;
      }
      test_results();
   
    }
    
    public void train_negitive(){
      negitive_training_counter += 1;
      negitive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
      negitive_training_alpha_avr = negitive_training_alpha_data / negitive_training_counter;
      
      negitive_training_beta_data += dataProcessing.headWidePower[BETA];
      negitive_training_beta_avr = negitive_training_beta_data / negitive_training_counter;
      
      fill(t2);
      noStroke();
      rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
      stroke(126);
      line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
      line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
      
      
      println("TN - Positive Training avr: " + positive_training_alpha_avr);
      println("TN - Negitive Training avr: " + negitive_training_alpha_avr);
      
    }
    
    public void train_positive(){
     
      
      

      if ((millis() - train_delay > last_delay) && (!testing)) {
          showColor = !showColor;
          last_delay = millis();
      }
      if (showColor) {
          fill(t1);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          positive_training_counter += 1;
          positive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
          positive_training_alpha_avr = positive_training_alpha_data / positive_training_counter;
      
          positive_training_beta_data += dataProcessing.headWidePower[BETA];
          positive_training_beta_avr = positive_training_beta_data / positive_training_counter;
          marker_indicater = 1;
          println("TP - Positive Training avr: " + positive_training_alpha_avr);
          if (dataProcessing.data_std_uV[0] > lookleft) lookleft = dataProcessing.data_std_uV[0];
          if (dataProcessing.data_std_uV[1] > lookleft1) lookleft1 = dataProcessing.data_std_uV[1];
      } else {
          fill(t2);
          noStroke();
          rect((int)(x) + 30, (int)(y) + 30, (int)(w) - 60, (int)(h) - 60);
          stroke(126);
          line((int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 - 20, (int)(x) + (int)(w) / 2, (int)(y) + (int)(h) / 2 + 20);
          line((int)(x) + (int)(w) / 2 - 20, (int)(y) + (int)(h) / 2, (int)(x) + (int)(w) / 2 + 20, (int)(y) + (int)(h) / 2);
          
          negitive_training_counter += 1;
          negitive_training_alpha_data += dataProcessing.headWidePower[ALPHA];
          negitive_training_alpha_avr = negitive_training_alpha_data / negitive_training_counter;
      
          negitive_training_beta_data += dataProcessing.headWidePower[BETA];
          negitive_training_beta_avr = negitive_training_beta_data / negitive_training_counter;
          marker_indicater = 0;
          println("TP - Negitive Training avr: " + negitive_training_alpha_avr);
      }
      
     
        
      
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)


    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        if (trainButton.isMouseHere()) {
            trainButton.setIsActive(true);
            println("Train Button pressed");
        } else trainButton.setIsActive(false);


        if (testButton.isMouseHere()) {
            testButton.setIsActive(true);
            println("Test Button pressed");
        } else testButton.setIsActive(false);
        
        if (cycleModeButton.isMouseHere()) {
            cycleModeButton.setIsActive(true);
            println("Cycle Mode Button pressed");
        } else cycleModeButton.setIsActive(false);

    }

    public void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
        if (trainButton != null && trainButton.isMouseHere()) {
            //do some function
            training = !training;
            if (training){
              testing = false;
            } else{
              marker_indicater = 0;}
            trainButton.wasPressed = true;
            verbosePrint("Training");
        }
        if (testButton != null && testButton.isMouseHere()) {
            //do some function
            testing = !testing;
            if (testing) training = false;
            testButton.wasPressed = true;
            verbosePrint("Testing");
        }
        
        if (cycleModeButton != null && cycleModeButton.isMouseHere()) {
            //do some function
            trainingMode = !trainingMode;            
            cycleModeButton.wasPressed = true;
            verbosePrint("Switching Modes");
        }
        
        if (clearTrainButton != null && clearTrainButton.isMouseHere()){
            verbosePrint("Clear Training Data");
            positive_training_alpha_avr = 0.0f;
            positive_training_alpha_data = 0.0f;
            negitive_training_alpha_avr = 0.0f;
            negitive_training_alpha_data = 0.0f;
            
            positive_training_beta_avr = 0.0f;
            positive_training_beta_data = 0.0f;
            negitive_training_beta_avr = 0.0f;
            negitive_training_beta_data = 0.0f;
        }
    }



    public void playVoice(int channel) {
        try {
            voice.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }



        switch (channel) {

            case 0:
                voice = soundMinim.loadFile(dataPath("m_yes.mp3"));
                voice.play();
                watchBeat = false;
                break;

        }

    }




};
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

  public void mouseDragged(){

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
      // println(e.getMessage());
      // println("widgetOptions List not built yet..."); AJK 8/22/17 because this is annoyance
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
W_networking w_networking;
W_BandPower w_bandPower;
W_accelerometer w_accelerometer;
W_ganglionImpedance w_ganglionImpedance;
W_headPlot w_headPlot;
W_template w_template1;
W_emg w_emg;
W_openBionics w_openbionics;
W_Focus w_focus;
W_PulseSensor w_pulsesensor;
W_AnalogRead w_analogRead;
W_DigitalRead w_digitalRead;
W_MarkerMode w_markermode;
W_matrix w_matrix;
W_darwin w_darwin;
W_Trainer w_trainer;



//ADD YOUR WIDGET TO WIDGETS OF WIDGETMANAGER
public void setupWidgets(PApplet _this, ArrayList<Widget> w){
  // println("  setupWidgets start -- " + millis());

  w_timeSeries = new W_timeSeries(_this);
  w_timeSeries.setTitle("Time Series");
  addWidget(w_timeSeries, w);
  // println("  setupWidgets time series -- " + millis());
  
  w_matrix = new W_matrix(_this);
  w_matrix.setTitle("Darwin Visual Matrix");
  addWidget(w_matrix, w);
  
  w_trainer = new W_Trainer(_this);
  w_trainer.setTitle("NN Trainer");
  addWidget(w_trainer, w);
  
  w_darwin = new W_darwin(_this);
  w_darwin.setTitle("Darwin Page");
  addWidget(w_darwin, w);
  
  w_fft = new W_fft(_this);
  w_fft.setTitle("FFT Plot");
  addWidget(w_fft, w);


 
  // println("  setupWidgets fft -- " + millis());

  w_accelerometer = new W_accelerometer(_this);
  w_accelerometer.setTitle("Accelerometer");
  w_networking = new W_networking(_this);
  w_networking.setTitle("Networking");

  //only instantiate this widget if you are using a Ganglion board for live streaming
  if(nchan == 4 && eegDataSource == DATASOURCE_GANGLION){
    w_ganglionImpedance = new W_ganglionImpedance(_this);
    w_ganglionImpedance.setTitle("Ganglion Signal");
    addWidget(w_ganglionImpedance, w);
    addWidget(w_networking, w);
    addWidget(w_accelerometer, w);
  } else {
    addWidget(w_accelerometer, w);
    addWidget(w_networking, w);
  }

  w_bandPower = new W_BandPower(_this);
  w_bandPower.setTitle("Band Power");
  addWidget(w_bandPower, w);
  // println("  setupWidgets band power -- " + millis());


  w_headPlot = new W_headPlot(_this);
  w_headPlot.setTitle("Head Plot");
  addWidget(w_headPlot, w);
  // println("  setupWidgets head plot -- " + millis());


  w_emg = new W_emg(_this);
  w_emg.setTitle("EMG");
  addWidget(w_emg, w);
  // println("  setupWidgets emg -- " + millis());


  w_focus = new W_Focus(_this);
  w_focus.setTitle("Focus Widget");
  addWidget(w_focus, w);
  // println("  setupWidgets focus widget -- " + millis());

  //only instantiate this widget if you are using a Cyton board for live streaming
  if(eegDataSource != DATASOURCE_GANGLION){
    w_pulsesensor = new W_PulseSensor(_this);
    w_pulsesensor.setTitle("Pulse Sensor");
    addWidget(w_pulsesensor, w);
    // println("  setupWidgets pulse sensor -- " + millis());

    w_digitalRead = new W_DigitalRead(_this);
    w_digitalRead.setTitle("Digital Read");
    addWidget(w_digitalRead, w);

    w_analogRead = new W_AnalogRead(_this);
    w_analogRead.setTitle("Analog Read");
    addWidget(w_analogRead, w);

    w_markermode = new W_MarkerMode(_this);
    w_markermode.setTitle("Marker Mode");
    addWidget(w_markermode, w);

  }

  w_template1 = new W_template(_this);
  w_template1.setTitle("Widget Template 1");
  addWidget(w_template1, w);

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

  public boolean isWMInitialized = false;
  private boolean visible = true;
  private boolean updating = true;

  WidgetManager(PApplet _this){
    widgets = new ArrayList<Widget>();
    widgetOptions = new ArrayList<String>();
    isWMInitialized = false;

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

    delay(1000);

    isWMInitialized = true;
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
    // if(visible && updating){
    if(visible){
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
        }else{
          if(widgets.get(i).widgetTitle.equals("Networking")){
            try{
              w_networking.shutDown();
            }catch (NullPointerException e){
              println(e);
            }
          }
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

  public void mouseDragged(){
    for(int i = 0; i < widgets.size(); i++){
      if(widgets.get(i).isActive){
        widgets.get(i).mouseDragged();
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
    layouts.add(new Layout(new int[]{1,7,15,16,17,18}));
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
  public void settings() {  size(1024, 768, P2D);  smooth(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "OpenBCI_GUI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
