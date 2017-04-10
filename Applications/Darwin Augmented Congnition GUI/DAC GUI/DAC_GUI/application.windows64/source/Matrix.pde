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

void MatrixSmoothEMG(int n){

  float samplesPerSecond;
  if(eegDataSource == DATASOURCE_GANGLION){
    samplesPerSecond = 200;
  } else {
    samplesPerSecond = 250;
  }

  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.01;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.1;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.15;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.25;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.5;
    }
    if(n == 5){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 0.75;
    }
    if(n == 6){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 1.0;
    }
    if(n == 7){
      w_matrix.motorWidgets[i].averagePeriod = samplesPerSecond * 2.0;
    }
  }
  closeAllDropdowns();
}

void MatrixuVLimit(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].acceptableLimitUV = 50.0;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].acceptableLimitUV = 100.0;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].acceptableLimitUV = 200.0;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].acceptableLimitUV = 400.0;
    }
  }
  closeAllDropdowns();
}

void MatrixCreepSpeed(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].creepSpeed = 0.9;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].creepSpeed = 0.95;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].creepSpeed = 0.98;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].creepSpeed = 0.99;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].creepSpeed = 0.999;
    }
  }
  closeAllDropdowns();
}

void MatrixminUVRange(int n){
  for(int i = 0 ; i < w_matrix.motorWidgets.length; i++){
    if(n == 0){
      w_matrix.motorWidgets[i].minRange = 10.0;
    }
    if(n == 1){
      w_matrix.motorWidgets[i].minRange = 20.0;
    }
    if(n == 2){
      w_matrix.motorWidgets[i].minRange = 40.0;
    }
    if(n == 3){
      w_matrix.motorWidgets[i].minRange = 80.0;
    }
    if(n == 4){
      w_matrix.motorWidgets[i].minRange = 5.0;
    }
    if(n == 5){
      w_matrix.motorWidgets[i].minRange = 1.0;
    }
    if(n == 6){
      w_matrix.motorWidgets[i].minRange = 0.05;
    }
    if(n == 7){
      w_matrix.motorWidgets[i].minRange = 18.0;
    }
    if(n == 8){
      w_matrix.motorWidgets[i].minRange = 15.0;
    }
    if(n == 9){
      w_matrix.motorWidgets[i].minRange = 30.0;
    }
    if(n == 10){
      w_matrix.motorWidgets[i].minRange = 25.0;
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
        motorWidgets[i].averagePeriod = 200 * 0.5;
      } else {
        motorWidgets[i].averagePeriod = 250 * 0.5;
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
  void initSliders(int rw, int rh) {
    //Stole some logic from the rectangle drawing in draw()
    int rowNum = 4;
    int colNum = motorWidgets.length / rowNum;
    int index = 0;

    float rowOffset = rh / rowNum;
    float colOffset = rw / colNum;

    for (int i = 0; i < rowNum; i++) {
      for (int j = 0; j < colNum; j++) {

        println("ROW: " + (4*rowOffset/8));
        tripSliders[index] = new TripSlider(int((5*colOffset/8) * 0.498), int((2 * rowOffset / 8) * 0.384), (4*rowOffset/8) * 0.408, int((3*colOffset/32) * 0.489), 2, tripSliders, true, motorWidgets[index]);
        untripSliders[index] = new TripSlider(int((5*colOffset/8) * 0.498), int((2 * rowOffset / 8) * 0.384), (4*rowOffset/8) * 0.408, int((3*colOffset/32) * 0.489), 2, tripSliders, false, motorWidgets[index]);
        //println("Slider :" + (j+i) + " first: " + int((5*colOffset/8) * 0.498)+ " second: " + int((2 * rowOffset / 8) * 0.384) + " third: " + int((3*colOffset/32) * 0.489));
        tripSliders[index].setStretchPercentage(motorWidgets[index].tripThreshold);
        untripSliders[index].setStretchPercentage(motorWidgets[index].untripThreshold);
        index++;
      }
    }
  }

  void update() {
    super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

    //put your code here...
    process(yLittleBuff_uV, dataBuffY_uV, dataBuffY_filtY_uV, fftBuff);
    //processTripps();
  }

  void draw() {
    
    
    super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

    //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class
    pushStyle();
    noStroke();
    fill(255);
    rect(x, y, w, h);

    if (MatrixAdvanced) {
      if (connectButton != null) connectButton.draw();
      else connectButton = new Button(int(x) + 2, int(y) - navHeight + 2, 35, navHeight - 6, "Conn", fontInfo.buttonLabel_size);
      if (soundButton != null) soundButton.draw();
      else soundButton = new Button(int(x) + 60, int(y) - navHeight + 2, 50, navHeight - 6, "Voice", fontInfo.buttonLabel_size);

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
    float scaleFactor = 1.0;
    float scaleFactorJaw = 1.5;
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

        int _x = int(3*colOffset/8);
        int _y = int(2 * rowOffset / 8);
        int _w = int(5*colOffset/32);
        int _h = int(4*rowOffset/8);

        //draw normalized bar graph of uV w/ matching channel color
        noStroke();
        fill(red(channelColors[index%8]), green(channelColors[index%8]), blue(channelColors[index%8]), 200);
        rect(_x, 3*_y + 1, _w, map(motorWidgets[i * colNum + j].output_normalized, 0, 1, 0, (-1) * int((4*rowOffset/8))));

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

  void screenResized() {
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

  void mousePressed() {
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

  void mouseReleased() {
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
      cfc.myAverage = 0.0;
      for (int i = data_forDisplay_uV[cfc.ourChan].length - int(cfc.averagePeriod); i < data_forDisplay_uV[cfc.ourChan].length; i++) {
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
        cfc.lowerThreshold = 1.0;
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
      cfc.output_adjusted = ((-0.1/(cfc.output_normalized*255.0)) + 255.0);



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
  void processTripps(){
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
    float myAverage = 0.0;            //this will change over time ... used for calculations below
    //prez related
    boolean switchTripped = false;
    int switchCounter = 0;
    float timeOfLastTrip = 0;
    float timeOfLastOff = 0;
    float tripThreshold = 0.75;
    float untripThreshold = 0.5;
    //if writing to a serial port
    int output = 0;                   //value between 0-255 that is the relative position of the current uV average between the rolling lower and upper uV thresholds
    float output_normalized = 0;      //converted to between 0-1
    float output_adjusted = 0;        //adjusted depending on range that is expected on the other end, ie 0-255?
    boolean analogBool = true;        //Analog events?
    boolean digitalBool = true;       //Digital events?

    //these are the 4 variables affected by the dropdown menus
    float averagePeriod; // = 125;          //number of data packets to average over (250 = 1 sec)
    float acceptableLimitUV = 200.0;    //uV values above this limit are excluded, as a result of them almost certainly being noise...
    float creepSpeed = 0.99;
    float minRange = 20.0;

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
    color current_color = color(255, 255, 255);
    Motor_Widget parent;

    //Constructor
    TripSlider(int ix, int iy, float il, int iwid, int ilen, TripSlider[] o, boolean wastrip, Motor_Widget p) {
      lx = ix;
      ly = iy;
      boxLen = int(il);
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
    void update(float tx, float ty) {
      // println("testing...");
      boxx = lx;
      //boxy = (wid + (ly/2)) - int(((wid + (ly/2)) - ly) * (float(stretch) / float(wid)));
      //boxy = ly + (ly - int( ly * (float(stretch) / float(wid)))) ;
      boxy = int(ly + stretch); //- stretch;

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
        int mappedVal = int(mouseY - (ty+ly));

        //println("bxLen: " + boxLen + " ty: " + ty + " ly: " + ly + " mouseY: " + mouseY + " boxy: " + boxy + " stretch: " + stretch + " width: " + wid + " mappedVal: " + mappedVal);

        if (!trip) stretch = lock(mappedVal, int(parent.untripThreshold * (boxLen)), boxLen);
        else stretch =  lock(mappedVal, 0, int(parent.tripThreshold * (boxLen)));

        if (mappedVal > boxLen && !trip) parent.tripThreshold = 1;
        else if (mappedVal > boxLen && trip) parent.untripThreshold = 1;
        else if (mappedVal < 0 && !trip) parent.tripThreshold = 0;
        else if (mappedVal < 0 && trip) parent.untripThreshold = 0;
        else if (!trip) parent.tripThreshold = float(mappedVal) / (boxLen);
        else if (trip) parent.untripThreshold = float(mappedVal) / (boxLen);
      }
    }

    //Checks if mouse is here
    void overEvent(float tx, float ty) {
      if (overRect(int(boxx + tx), int(boxy + ty), wid, len)) {
        over = true;
      } else {
        over = false;
      }
    }

    //Checks if mouse is pressed
    void pressEvent() {
      if (over && mousePressed || locked) {
        press = true;
        locked = true;
      } else {
        press = false;
      }
    }

    //Mouse was released
    void releaseEvent() {
      locked = false;
    }

    //Color selector and cursor setter
    void setColor() {
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
    void setStretchPercentage(float val) {
      stretch = lock(int(boxLen - ((boxLen) * val)), 0, boxLen);
    }

    //Displays the thresholds %%%%%
    void display(float tx, float ty, float tw, float tl) {
      lx = int(tx);
      ly = int(ty);
      wid = int(tw);
      boxLen = int(tl);

      fill(255);
      strokeWeight(1);
      stroke(bgColor);
      setColor();
      fill(current_color);
      rect(boxx, boxy, wid, len);

      // rect(lx, ly, wid, len);
    }

    //Check if the mouse is here
    boolean overRect(int lx, int ly, int twidth, int theight) {
      if (mouseX >= lx && mouseX <= lx+twidth &&
        mouseY >= ly && mouseY <= ly+theight) {

        return true;
      } else {
        return false;
      }
    }

    //Locks the threshold in place
    int lock(int val, int minv, int maxv) {
      return  min(max(val, minv), maxv);
    }
  };


  void sendSerial(){
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
        first += int(serialChannels[b]) << b;
        //first8 = 1 << b;
          
      }
       //println("first8 " +binary(first));
      for(int t = 0 ; t < 8; t++){
        
        second += int(serialChannels[t+8]) << t;
          
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
        int j = int(binary(0000000000000001));
        //serialOutMatrix.write(first);
      }
      catch(RuntimeException e){
         
      }
      
      
      //serialOutMatrix.write(serialChannels[w_matrix.motorWidgets.length]);
      //serialOutMatrix.write(endMarker);
      
    }
  }
  void playVoice(int channel){
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
   void digitalEventChanHander(int channel,Motor_Widget cfc) {
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
  void analogEventChan0(Motor_Widget cfc) {

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
  void analogEventChan1(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 3 Event
  void analogEventChan2(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 4 Event
  void analogEventChan3(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 5 Event
  void analogEventChan4(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 6 Event
  void analogEventChan5(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 7 Event
  void analogEventChan6(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 8 Event
  void analogEventChan7(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 9 Event
  void analogEventChan8(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 10 Event
  void analogEventChan9(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 11 Event
  void analogEventChan10(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 12 Event
  void analogEventChan11(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 13 Event
  void analogEventChan12(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 14 Event
  void analogEventChan13(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 15 Event
  void analogEventChan14(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }

  //Channel 16 Event
  void analogEventChan15(Motor_Widget cfc) {

    float output_normalized = cfc.output_normalized;
    float tripThreshold = cfc.tripThreshold;
    float untripThreshold = cfc.untripThreshold;
    boolean switchTripped = cfc.switchTripped;
    float timeOfLastTrip = cfc.timeOfLastTrip;
  }
};


void MatrixChannelSelection(int n) {
  w_matrix.currChannel = n;
  closeAllDropdowns();
}

void MatrixEventType(int n) {
  if (n == 0) w_matrix.events[w_matrix.currChannel] = true;
  else if (n == 1) w_matrix.events[w_matrix.currChannel] = false;
  closeAllDropdowns();
}

void MatrixBaudRate(int n) {
  if (!w_matrix.baudList.get(n).equals("NONE")) w_matrix.theBaud = Integer.parseInt(w_matrix.baudList.get(n));
  closeAllDropdowns();
}

void MatrixSerialSelection(int n) {
  if (!w_matrix.serList.get(n).equals("NONE")) w_matrix.theSerial = w_matrix.serList.get(n);
  //w_matrix.theSerial = w_matrix.serList.get(n);
  println(w_matrix.theSerial);
  closeAllDropdowns();
}