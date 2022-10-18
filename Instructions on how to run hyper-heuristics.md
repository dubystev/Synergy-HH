To run the hyper-heuristics in the src/hh_project directory<br/>
1.&nbsp;&nbsp;&nbsp;Copy all the codes into the same source directory of your Java project

2a.&nbsp;Make sure you edit the following line<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;args = new String[]{"1", "5", "6", "31", "507000"};<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;in hh_run_batch.java to suit the algorithm you intend to test-run on the HyFlex domains.<br/>
2b. For example, the string aray element above can be explained as follows: The tester wants to run EA-ILS ("1") on the 6th instance ("6") of the 
    PS domain ("5") 31 times ("31"). Each run will be stopped after 507 secs ("507000").<br/>
2c. To further enhance understanding, the switch statement starting with the expression "switch(Domain){" controls which domain is used to test an algorithm
    while the other switch statment within the method void setHH() which is towards the end of hh_run_batch.java triggers the algorithm to run based on the
    integer parameter entered by the user in the <b>args</b> array.<br/>

3.  Notice there are two algorithms to choose from: TS-ILS (ILS_conf.java) and EA-ILS (EA_ILS_final.java). The switch statment in the setHH() method can be
    extended if there is a need to test another algorithm in the source code folder<br/><br/>
    
4.  When you are set, run hh_run_batch.java (since it is the java file that has a main method) and wait until all the trials are completed.
