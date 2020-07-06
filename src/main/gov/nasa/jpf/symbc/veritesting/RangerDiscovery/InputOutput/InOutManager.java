package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Config;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.NamedFields;
import gov.nasa.jpf.vm.StackFrame;
import jkind.lustre.*;

import java.util.ArrayList;


/**
 * this class manages the input and output of RNode, it assumes that the input and the output of the "step" function is
 * provided, it is divided into 4 types, freeInput, stateInput, stateOutput and contractOutput. The type of those
 * should match the signature of the step function. Type conversion is needed sometimes, if so then the variable
 * names in the arraylist will change to the new var being created, in this case there will be as well a side effect
 * for the equations needed for conversion between the original var and the new var being created for conversion.
 * <p>
 * An important thing to note here is that the signature of the different input, output, or state are reflecting those
 * in the implementation type.
 */

/**
 * State output refers to any state variable that the class keeps track of in each iteration, it really depends on
 * the implementation.
 * Contract output on the other hand refers to outputs that the specification are going to use to check specific
 * constraints. With this definition, contract output has nothing to do with the actual output of the contract in the
 * implementation, for the specification can be checking really multiple things.
 * <p>
 * There is an overlap between state output and contract output, when plugging in things we need to be careful about
 * what each term means. i.e., for those state variables that are going to be checked with the specification, even
 * though they are part of the state and therefore might be considered as state output, they are however checked by
 * the sepecification and with this regard they are defined as contract output instead. They should not be included as
 * a state output, only as a contract output.
 */
public class InOutManager {

    //this number is very important it should be the same between the passed inputs into the spec that we think is an
    // output of the model and it must also be the same size as the list in contractOutput
    public static int wrapperOutputNum;

    Input freeInput = new Input();
    Input stateInput = new Input();

    //This is the state output of the class in the implementation.
    ContractOutput stateOutput = new ContractOutput();


    //This describes the output that is going to be validated with the specification, they are usually part of the
    // state but should NOT be mistaken as a stateOutput, a stateOutput are only those needed internally for R node
    // and are not validated by the spec, for those that needs to be  validated by the spec we call the
    // contractOutput and must be populated there.

    //To identify contract output we rely on two important assumptions. First, the contract output is already defined in the spec
    // definition, and secondly, the contract input and outputs are always the LAST variables in the stateInput and stateOutput
    // lists. So when we try to popluate the contract output we use these above information, then we clear the stateInput and stateOutputs
    //from any contract input and output fields, we can do that by removing them from the end of the list, since we know who many they are
    //from the first assumptions and since they are always at the end of the stateInput and stateOutput lists from the second assumption.
    ContractOutput contractOutput = new ContractOutput();


    boolean isOutputConverted = false;

    //carries any type conversion equation which can be triggered both in case of the input and the output
    ArrayList<Equation> typeConversionEq = new ArrayList<>();

    ArrayList<VarDecl> conversionLocalList = new ArrayList<>();

    private String referenceObjectName;
    private String referenceObjectName_gpca_Alarm_Outputs;
    private String referenceObjectName_gpca_localB;
    private String referenceObjectName_gpca_localDW;


    //specific reference names for Infusion
    private String referenceObjectName_infusion_Outputs;
    private String referenceObjectName_infusion_localB;
    private String referenceObjectName_infusion_localDW;
    private SpecInOutManager specInOutManager;


    public InOutManager() {
        if (Config.mac) {
            referenceObjectName = "r347"; //for mac

            //specific reference names for GPCA
            referenceObjectName_gpca_Alarm_Outputs = "r390";
            referenceObjectName_gpca_localB = "r394";
            referenceObjectName_gpca_localDW = "r398";

            //specific reference names for Infusion
            referenceObjectName_infusion_Outputs = "r382";
            referenceObjectName_infusion_localB = "r350";
            referenceObjectName_infusion_localDW = "r354";

        } else { //assume linux
            referenceObjectName = "r351"; //for lunix

            //specific reference names for GPCA
            referenceObjectName_gpca_Alarm_Outputs = "r394";
            referenceObjectName_gpca_localB = "r398";
            referenceObjectName_gpca_localDW = "r402";


            //specific reference names for Infusion
            referenceObjectName_infusion_Outputs = "r386";
            referenceObjectName_infusion_localB = "r354";
            referenceObjectName_infusion_localDW = "r358";

        }
    }


    public ArrayList<Equation> getTypeConversionEq() {
        return typeConversionEq;
    }

    public ArrayList<VarDecl> getConversionLocalList() {
        return conversionLocalList;
    }

    public boolean isOutputConverted() {
        return isOutputConverted;
    }

    // this is now here to determine the name of the symVar, since in the new SPF they have adapted a new naming for
    // symbolic variables for which we need a dynamic mechanism to get the right name. This can be handled in a
    // later stage.
    public static String setSymVarName() {
        if (Config.spec.equals("wbs")) Config.symVarName = "symVar_7_SYMINT";
        else if (Config.spec.equals("tcas")) Config.symVarName = "symVar_13_SYMINT";
        else if (Config.spec.equals("vote")) Config.symVarName = "symVar_6_SYMINT";
        else if (Config.spec.equals("gpca")) Config.symVarName = "symVar_212_SYMINT";
        else if (Config.spec.equals("infusion")) Config.symVarName = "symVar_103_SYMINT";
        else assert false;
        return Config.symVarName;
    }

    //* IMPORTANT!!! the order of variables of state input should match those  of variables of the state output!!*//
    public void discoverVars(SpecInOutManager tInOutManager) {
        setSymVarName();
        this.specInOutManager = tInOutManager;

        if (Config.spec.equals("pad")) {
            discoverFreeInputPad();
            doFreeTypeConversion();

            discoverStateInputPad();
            doStateInputTypeConversion();

            discoverStateOutputPad();
            doStateOutputTypeConversion();

            discoverContractOutputPad();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("even")) {
            discoverFreeInputEven();
            doFreeTypeConversion();

            discoverStateInputEven();
            doStateInputTypeConversion();

            discoverStateOutputEven();
            doStateOutputTypeConversion();

            discoverContractOutputEven();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("wbs")) {
            discoverFreeInputWBS();
            doFreeTypeConversion();

            discoverStateInputWBS();
            doStateInputTypeConversion();

            discoverStateOutputWBS();
            doStateOutputTypeConversion();

            discoverContractOutputWBS();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("tcas")) {
            discoverFreeInputTCAS();
            doFreeTypeConversion();

            discoverStateInputTCAS();
            doStateInputTypeConversion();

            discoverStateOutputTCAS();
            doStateOutputTypeConversion();

            discoverContractOutputTCAS();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("gpca")) {
            discoverFreeInputGPCA();
            doFreeTypeConversion();

            discoverStateInputGPCA();
            doStateInputTypeConversion();

            discoverStateOutputGPCA();
            doStateOutputTypeConversion();

            discoverContractOutputGPCA();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("infusion")) {
            discoverFreeInputInfusion();
            doFreeTypeConversion();

            discoverStateInputInfusion();
            doStateInputTypeConversion();

            discoverStateOutputInfusion();
            doStateOutputTypeConversion();

            discoverContractOutputInfusion();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("vote")) {
            discoverFreeInputVote();
            doFreeTypeConversion();

            discoverStateInputVote();
            doStateInputTypeConversion();

            discoverStateOutputVote();
            doStateOutputTypeConversion();

            discoverContractOutputVote();
            doContractOutputTypeConversion();

        } else if (Config.spec.equals("vote2")) {
            discoverFreeInputVote2();
            doFreeTypeConversion();

            discoverStateInputVote2();
            doStateInputTypeConversion();

            discoverStateOutputVote2();
            doStateOutputTypeConversion();

            discoverContractOutputVote2();
            doContractOutputTypeConversion();


        } else {
            System.out.println("unexpected spec to run.!");
            assert false;
        }
        wrapperOutputNum = contractOutput.size;

        checkAsserts();
    }

    private void checkAsserts() {
        assert contractOutput.varInitValuePair.size() == 0; // contract cannot have initial values
        assert contractOutput.varList.size() != 0; // an implementation must define a contract output.
        assert stateOutput.varInitValuePair.size() == stateOutput.varList.size();
        assert freeInput.size > 0;
        assert wrapperOutputNum == contractOutput.size;

    }

    //================================= Type Conversion ========================

    private void doContractOutputTypeConversion() {
        if (contractOutput.containsBool()) {
            ArrayList<Equation> conversionResult = contractOutput.convertContractOutput();
            assert conversionResult.size() == 1;
            typeConversionEq.addAll(conversionResult);
            isOutputConverted = true;
        }
    }

    private void doFreeTypeConversion() {
        if (freeInput.containsBool()) {
            Pair<ArrayList<VarDecl>, ArrayList<Equation>> conversionResult = freeInput.convertInput();
            typeConversionEq.addAll(conversionResult.getSecond());
            conversionLocalList.addAll(conversionResult.getFirst());
        }
    }

    private void doStateInputTypeConversion() {
        if (stateInput.containsBool()) { //type conversion to spf int type is needed
            Pair<ArrayList<VarDecl>, ArrayList<Equation>> conversionResult = stateInput.convertInput();
            typeConversionEq.addAll(conversionResult.getSecond());
            conversionLocalList.addAll(conversionResult.getFirst());
        }
    }

    private void doStateOutputTypeConversion() {
        if (stateOutput.containsBool()) {
            ArrayList<Equation> conversionResult = stateOutput.convertOutput();
            typeConversionEq.addAll(conversionResult);
            //conversionLocalList.addAll(conversionResult.getFirst()); // no need to add this, since these are already as
            // def in the dynStmt
            isOutputConverted = true;
        }
    }

    //================================= end Type Conversion ========================


    //================================= Pad ========================
    //entered by hand for now -- this is a singleton, I need to enforce this everywhere.
    private void discoverContractOutputPad() {
        contractOutput.add(referenceObjectName + ".ignition_r.1.7.4", NamedType.BOOL);
//        contractOutput.addInit(referenceObjectName + ".ignition_r.1.7.4", new BoolExpr(false));
    }


    //entered by hand for now
    private void discoverFreeInputPad() {
        freeInput.add("signal", NamedType.INT);
    }


    //entered by hand for now
    private void discoverStateInputPad() {
        stateInput.add("start_btn", NamedType.BOOL);
        stateInput.add("launch_btn", NamedType.BOOL);
        stateInput.add("reset_btn", NamedType.BOOL);
        stateInput.add("ignition", NamedType.BOOL);

    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputPad() {
        stateOutput.add(referenceObjectName + ".start_btn.1.15.4", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName + ".start_btn.1.15.4", new BoolExpr(false));

        stateOutput.add(referenceObjectName + ".launch_btn.1.17.4", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName + ".start_btn.1.15.4", new BoolExpr(false));

        stateOutput.add(referenceObjectName + ".reset_btn.1.9.4", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName + ".start_btn.1.15.4", new BoolExpr(false));

    }


    //====================== WBS ====================================


    //entered by hand for now - this defines the output that we expect to validate with the T_node,i.e, this is the
    // output of the wrapper that gets plugged in the T_node to  validate it. Therefore it is not directly reflecting
    // the method output of the implementation, instead it is the output of the to-be-created r_wrapper node.

    private void discoverContractOutputWBS() {
        //defines that using the output defined for the spec
        int specOutputSize = specInOutManager.getInOutput().size;
        int implStateOutSize = stateOutput.varList.size();

        for (int i = implStateOutSize - specOutputSize; i < implStateOutSize; i++) {
            Pair<String, NamedType> outputPair = stateOutput.varList.get(i);
            contractOutput.add(outputPair.getFirst(), outputPair.getSecond());
//            contractOutput.addInit(outputPair.getFirst(), outputPair.getSecond() == NamedType.INT ? new IntExpr(0) : new BoolExpr(false));
        }

        //commented out since for now we have added contractInput to handle this case in the stateOutput, so we do no have to clear it here.
        for (int i = 0; i < specOutputSize; i++) {
            int currIndex = stateOutput.varList.size() - 1;
            stateOutput.remove(currIndex); //remove last element as many times as the contract output of the spec
            stateOutput.removeInit(currIndex);
            stateInput.remove(currIndex); //remove last element as many times as the contract output of the spec
        }

/*

        contractOutput.add(referenceObjectName + ".Nor_Pressure.1.13.2", NamedType.INT);
//        contractOutput.addInit(referenceObjectName + ".Nor_Pressure.1.13.2", new IntExpr(0));

        contractOutput.add(referenceObjectName + ".Alt_Pressure.1.13.2", NamedType.INT);
//        contractOutput.addInit(referenceObjectName + ".Alt_Pressure.1.13.2", new IntExpr(0));

        contractOutput.add(referenceObjectName + ".Sys_Mode.1.5.2", NamedType.INT);
//        contractOutput.addInit(referenceObjectName + ".Sys_Mode.1.5.2", new IntExpr(0));
*/

    }

    //entered by hand for now
    private void discoverFreeInputWBS() {

        StackFrame sf = Config.ti.getTopFrame();
        Object[] symbolicInputs = sf.getSlotAttrs();
        int indexOfLastSym = 0;
        boolean symVarFound = false; // to work around having the symVarAttr appearing twice in the stackslot, not sure why the duplication is happening, this is why this is a workaround rather than removing the double entry
        for (int i = 0; i < symbolicInputs.length; i++) {
            Object symInput = symbolicInputs[i];
            if (symInput != null)
                if (!symVarFound || !symInput.toString().contains("symVar")) {
                    if (symInput.toString().contains("symVar"))
                        symVarFound = true;
                    indexOfLastSym = i;
                }
        }
        for (int i = 0; i < indexOfLastSym; i++) {
            Object symInput = symbolicInputs[i];
            if (symInput != null) {
                char argType = sf.getMethodInfo().getSignature().charAt(i + 1);
                if (argType == 'I')
                    freeInput.add(symbolicInputs[i].toString(), NamedType.INT);
                else if (argType == 'Z')
                    freeInput.add(symbolicInputs[i].toString(), NamedType.BOOL);
                else
                    assert false : "unexpected type.Failing";
            }
        }

        /*freeInput.add("pedal_1_SYMINT", NamedType.INT);
        freeInput.add("autoBrake_2_SYMINT", NamedType.BOOL);
        freeInput.add("skid_3_SYMINT", NamedType.BOOL);
*/
        /*if (freeInput.containsBool()) {
            Pair<ArrayList<VarDecl>, ArrayList<Equation>> conversionResult = freeInput.convertInput();
            typeConversionEq.addAll(conversionResult.getSecond());
            conversionLocalList.addAll(conversionResult.getFirst());
        }*/
    }

    // IMP: order here is important. Firs the put the input variables for the state, then the state variables that account
// for the outputs of the sepc.
    //entered by hand for now
    private void discoverStateInputWBS() {
        for (ElementInfo ei : Config.objrefs) {
            int fieldCount = ei.getClassInfo().getDeclaredInstanceFields().length;
            for (int i = 0; i < fieldCount; i++) {
                ObjectList.Iterator fieldAttrItr = ((NamedFields) (Config.objrefs.get(0).getFields())).fieldAttrIterator(i);
                Object fieldAttr = fieldAttrItr.next();
                String fieldType = ei.getClassInfo().getDeclaredInstanceFields()[i].getType();
                NamedType namedType = fieldType.equals("int") ? NamedType.INT : NamedType.BOOL;
                if (namedType == NamedType.BOOL)
                    assert fieldType.equals("boolean");
                //if (!contractInput.contains(fieldAttr.toString(), namedType)) //filtering out contractInput from being counted as a state
                    stateInput.add(fieldAttr.toString(), namedType);
            }
        }

        /*stateInput.add("WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE_4_SYMINT", NamedType.INT);
        stateInput.add("WBS_Node_WBS_BSCU_rlt_PRE1_5_SYMINT", NamedType.INT);
        stateInput.add("WBS_Node_WBS_rlt_PRE2_6_SYMINT", NamedType.INT);
*/
    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputWBS() {
        for (Pair<String, NamedType> stateIn : stateInput.varList) {
            String lastSSA = LastSSAVisitor.execute(DiscoverContract.dynRegion.dynStmt, stateIn.getFirst());
            NamedType namedType = stateIn.getSecond();
            stateOutput.add(lastSSA, namedType);
            if (namedType == NamedType.INT)
                stateOutput.addInit(lastSSA, new IntExpr(0));
            else if (namedType == NamedType.BOOL)
                stateOutput.addInit(lastSSA, new BoolExpr(false));
            else
                assert false : "unexpected type for stateoutput. Failing";
        }

        /*stateOutput.add(referenceObjectName + ".WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE.1.3.2", NamedType.INT);
        stateOutput.addInit(referenceObjectName + ".WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE.1.3.2", new IntExpr(0));


        stateOutput.add(referenceObjectName + ".WBS_Node_WBS_BSCU_rlt_PRE1.1.3.2", NamedType.INT);
        stateOutput.addInit(referenceObjectName + ".WBS_Node_WBS_BSCU_rlt_PRE1.1.3.2", new IntExpr(0));


        stateOutput.add(referenceObjectName + ".WBS_Node_WBS_rlt_PRE2.1.3.2", NamedType.INT);
        stateOutput.addInit(referenceObjectName + ".WBS_Node_WBS_rlt_PRE2.1.3.2", new IntExpr(0));
*/
    }


    //====================== TCAS ====================================


    //entered by hand for now - this defines the output that we expect to validate with the T_node,i.e, this is the
    // output of the wrapper that gets plugged in the T_node to  validate it. Therefore it is not directly reflecting
    // the method output of the implementation, instead it is the output of the to-be-created r_wrapper node.

    private void discoverContractOutputTCAS() {

        contractOutput.add("r-1.result_alt_sep_test.1.3.33", NamedType.INT);
//        contractOutput.addInit("r-1.result_alt_sep_test.1.4.33", new IntExpr(0));

        contractOutput.add("r-1.alim_res.1.3.33", NamedType.INT);
//        contractOutput.addInit("r-1.alim_res.1.4.33", new IntExpr(0));
    }


    //entered by hand for now
    private void discoverFreeInputTCAS() {
        freeInput.add("Cur_Vertical_Sep_1_SYMINT", NamedType.INT);
        freeInput.add("High_Confidence_flag_2_SYMINT", NamedType.INT);
        freeInput.add("Two_of_Three_Reports_Valid_flag_3_SYMINT", NamedType.INT);
        freeInput.add("Own_Tracked_Alt_4_SYMINT", NamedType.INT);
        freeInput.add("Own_Tracked_Alt_Rate_5_SYMINT", NamedType.INT);
        freeInput.add("Other_Tracked_Alt_6_SYMINT", NamedType.INT);
        freeInput.add("Alt_Layer_Value_7_SYMINT", NamedType.INT);
        freeInput.add("Up_Separation_8_SYMINT", NamedType.INT);
        freeInput.add("Down_Separation_9_SYMINT", NamedType.INT);
        freeInput.add("Other_RAC_10_SYMINT", NamedType.INT);
        freeInput.add("Other_Capability_11_SYMINT", NamedType.INT);
        freeInput.add("Climb_Inhibit_12_SYMINT", NamedType.INT);
    }

    //entered by hand for now
    private void discoverStateInputTCAS() {

        /*stateInput.add("High_Confidence", NamedType.INT);
        stateInput.add("Two_of_Three_Reports_Valid", NamedType.INT);
*/
  /*      stateInput.add("result_alt_sep_test_13_SYMINT", NamedType.INT);
        stateInput.add("alim_res_14_SYMINT", NamedType.INT);*/
    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputTCAS() {

        /*stateOutput.add("r-1.High_Confidence.1.5.25", NamedType.INT);
        stateOutput.addInit("r-1.High_Confidence.1.5.25", new IntExpr(0));

        stateOutput.add("r-1.Two_of_Three_Reports_Valid.1.5.25", NamedType.INT);
        stateOutput.addInit("r-1.Two_of_Three_Reports_Valid.1.5.25", new IntExpr(0));*/
    }

//====================== GPCA ====================================


    private void discoverContractOutputGPCA() {

        contractOutput.add(referenceObjectName_gpca_Alarm_Outputs + ".Is_Audio_Disabled.1.3.51", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_gpca_Alarm_Outputs + ".Is_Audio_Disabled.1.3.51", new IntExpr(0));

        contractOutput.add(referenceObjectName_gpca_Alarm_Outputs + ".Notification_Message.1.3.51", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_gpca_Alarm_Outputs + ".Notification_Message.1.3.51", new IntExpr(0));

        contractOutput.add(referenceObjectName_gpca_Alarm_Outputs + ".Audio_Notification_Command.1.3.51", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_gpca_Alarm_Outputs + ".Audio_Notification_Command.1.3.51", new IntExpr(0));

        contractOutput.add(referenceObjectName_gpca_Alarm_Outputs + ".Highest_Level_Alarm.1.3.51", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_gpca_Alarm_Outputs + ".Highest_Level_Alarm.1.3.51", new IntExpr(0));

        contractOutput.add(referenceObjectName_gpca_Alarm_Outputs + ".Log_Message_ID.1.3.51", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_gpca_Alarm_Outputs + ".Log_Message_ID.1.3.51", new IntExpr(0));

    }

    //entered by hand for now -- this is not completely matching the signature of Alarm_FunctionalSymWrapper because
    // some of the inputs are not used and therefore they do not show up in the linearized form.
    private void discoverFreeInputGPCA() {
        freeInput.add("Commanded_Flow_Rate_1_SYMINT", NamedType.INT);
        freeInput.add("Current_System_Mode_2_SYMINT", NamedType.INT);
        freeInput.add("System_On_6_SYMINT", NamedType.BOOL);
        freeInput.add("System_Monitor_Failed_9_SYMINT", NamedType.BOOL);
        freeInput.add("Logging_Failed_11_SYMINT", NamedType.BOOL);
        freeInput.add("Infusion_Initiate_14_SYMINT", NamedType.BOOL);
        freeInput.add("Disable_Audio_22_SYMINT", NamedType.INT);
        freeInput.add("Notification_Cancel_23_SYMINT", NamedType.BOOL);
        freeInput.add("VTBI_High_30_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_High_35_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_Low_36_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_37_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_Not_Stable_38_SYMINT", NamedType.BOOL);
        freeInput.add("Air_In_Line_39_SYMINT", NamedType.BOOL);
        freeInput.add("Occlusion_40_SYMINT", NamedType.BOOL);
        freeInput.add("Door_Open_41_SYMINT", NamedType.BOOL);
        freeInput.add("Temp_42_SYMINT", NamedType.BOOL);
        freeInput.add("Air_Pressure_43_SYMINT", NamedType.BOOL);
        freeInput.add("Humidity_44_SYMINT", NamedType.BOOL);
        freeInput.add("Battery_Depleted_45_SYMINT", NamedType.BOOL);
        freeInput.add("Battery_Low_46_SYMINT", NamedType.BOOL);
        freeInput.add("Battery_Unable_To_Charge_47_SYMINT", NamedType.BOOL);
        freeInput.add("Supply_Voltage_48_SYMINT", NamedType.BOOL);
        freeInput.add("CPU_In_Error_49_SYMINT", NamedType.BOOL);
        freeInput.add("RTC_In_Error_50_SYMINT", NamedType.BOOL);
        freeInput.add("Watchdog_Interrupted_51_SYMINT", NamedType.BOOL);
        freeInput.add("Memory_Corrupted_52_SYMINT", NamedType.BOOL);
        freeInput.add("Pump_Too_Hot_53_SYMINT", NamedType.BOOL);
        freeInput.add("Pump_Overheated_54_SYMINT", NamedType.BOOL);
        freeInput.add("Audio_Enable_Duration_57_SYMINT", NamedType.INT);
        freeInput.add("Audio_Level_58_SYMINT", NamedType.INT);
        freeInput.add("Config_Warning_Duration_59_SYMINT", NamedType.INT);
        freeInput.add("Low_Reservoir_61_SYMINT", NamedType.INT);
        freeInput.add("Max_Duration_Over_Infusion_63_SYMINT", NamedType.INT);
        freeInput.add("Max_Duration_Under_Infusion_64_SYMINT", NamedType.INT);
        freeInput.add("Max_Paused_Duration_65_SYMINT", NamedType.INT);
        freeInput.add("Max_Idle_Duration_66_SYMINT", NamedType.INT);
        freeInput.add("Tolerance_Max_67_SYMINT", NamedType.INT);
        freeInput.add("Tolerance_Min_68_SYMINT", NamedType.INT);
        freeInput.add("Reservoir_Empty_73_SYMINT", NamedType.BOOL);
        freeInput.add("Reservoir_Volume1_74_SYMINT", NamedType.INT);
        freeInput.add("Volume_Infused_75_SYMINT", NamedType.INT);
        freeInput.add("In_Therapy_77_SYMINT", NamedType.BOOL);
        freeInput.add("Config_Timer_101_SYMINT", NamedType.INT);

        /*if (freeInput.containsBool()) {
            Pair<ArrayList<VarDecl>, ArrayList<Equation>> conversionResult = freeInput.convertInput();
            typeConversionEq.addAll(conversionResult.getSecond());
            conversionLocalList.addAll(conversionResult.getFirst());
        }*/
    }

    //entered by hand for now
    private void discoverStateInputGPCA() {

        //localB
        stateInput.add("localB_Commanded_Flow_Rate_103_SYMINT", NamedType.INT);
        stateInput.add("localB_Current_System_Mode_104_SYMINT", NamedType.INT);
        stateInput.add("localB_Disable_Audio_105_SYMINT", NamedType.INT);
        stateInput.add("localB_VTBI_High_106_SYMINT", NamedType.INT);
        stateInput.add("localB_Flow_Rate_High_107_SYMINT", NamedType.INT);
        stateInput.add("localB_Flow_Rate_Low_108_SYMINT", NamedType.INT);
        stateInput.add("localB_Flow_Rate_109_SYMINT", NamedType.INT);


        stateInput.add("localB_Audio_Enable_Duration_110_SYMINT", NamedType.INT);
        stateInput.add("localB_Audio_Level_111_SYMINT", NamedType.INT);
        stateInput.add("localB_Config_Warning_Duration_112_SYMINT", NamedType.INT);
        stateInput.add("localB_Low_Reservoir_113_SYMINT", NamedType.INT);
        stateInput.add("localB_Max_Duration_Over_Infusion_114_SYMINT", NamedType.INT);
        stateInput.add("localB_Max_Duration_Under_Infusion_115_SYMINT", NamedType.INT);
        stateInput.add("localB_Max_Paused_Duration_116_SYMINT", NamedType.INT);
        stateInput.add("localB_Max_Idle_Duration_117_SYMINT", NamedType.INT);
        stateInput.add("localB_Tolerance_Max_118_SYMINT", NamedType.INT);
        stateInput.add("localB_Tolerance_Min_119_SYMINT", NamedType.INT);
        stateInput.add("localB_Reservoir_Volume_120_SYMINT", NamedType.INT);
        stateInput.add("localB_Volume_Infused_121_SYMINT", NamedType.INT);
        stateInput.add("localB_Config_Timer_122_SYMINT", NamedType.INT);
        stateInput.add("localB_ALARM_OUT_Display_Audio_Disabled_Indicator_123_SYMINT", NamedType.INT);
        stateInput.add("localB_ALARM_OUT_Display_Notification_Command_124_SYMINT", NamedType.INT);
        stateInput.add("localB_ALARM_OUT_Audio_Notification_Command_125_SYMINT", NamedType.INT);
        stateInput.add("localB_ALARM_OUT_Highest_Level_Alarm_126_SYMINT", NamedType.INT);
        stateInput.add("localB_ALARM_OUT_Log_Message_ID_127_SYMINT", NamedType.INT);
        stateInput.add("localB_System_On_128_SYMINT", NamedType.BOOL);
        stateInput.add("localB_System_Monitor_Failed_129_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Logging_Failed_130_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Infusion_Initiate_131_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Notification_Cancel_132_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Flow_Rate_Not_Stable_133_SYMINT", NamedType.BOOL);

        stateInput.add("localB_Air_In_Line_134_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Occlusion_135_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Door_Open_136_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Temp_137_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Air_Pressure_138_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Humidity_139_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Battery_Depleted_140_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Battery_Low_141_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Battery_Unable_To_Charge_142_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Supply_Voltage_143_SYMINT", NamedType.BOOL);
        stateInput.add("localB_CPU_In_Error_144_SYMINT", NamedType.BOOL);
        stateInput.add("localB_RTC_In_Error_145_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Watchdog_Interrupted_146_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Memory_Corrupted_147_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Pump_Too_Hot_148_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Pump_Overheated_149_SYMINT", NamedType.BOOL);
        stateInput.add("localB_Reservoir_Empty_150_SYMINT", NamedType.BOOL);
        stateInput.add("localB_In_Therapy_151_SYMINT", NamedType.BOOL);

//localDW
        stateInput.add("is_active_c2_ALARM_Functional_152_SYMINT", NamedType.INT);
        stateInput.add("is_c2_ALARM_Functional_153_SYMINT", NamedType.INT);
        stateInput.add("is_active_Notification_154_SYMINT", NamedType.INT);
        stateInput.add("is_Visual_155_SYMINT", NamedType.INT);
        stateInput.add("is_active_Visual_156_SYMINT", NamedType.INT);
        stateInput.add("is_Audio_157_SYMINT", NamedType.INT);
        stateInput.add("is_active_Audio_158_SYMINT", NamedType.INT);
        stateInput.add("is_active_CheckAlarm_159_SYMINT", NamedType.INT);

        //here
        stateInput.add("is_CancelAlarm_160_SYMINT", NamedType.INT);
        stateInput.add("is_active_CancelAlarm_161_SYMINT", NamedType.INT);
        stateInput.add("is_active_SetAlarmStatus_162_SYMINT", NamedType.INT);
        stateInput.add("is_active_Level4_163_SYMINT", NamedType.INT);
        stateInput.add("is_IsEmptyReservoir_164_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsEmptyReservoir_165_SYMINT", NamedType.INT);
        stateInput.add("is_IsSystemMonitorFailed_166_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsSystemMonitorFailed_167_SYMINT", NamedType.INT);
        stateInput.add("is_IsEnviromentalError_168_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsEnviromentalError_169_SYMINT", NamedType.INT);
        stateInput.add("is_active_Level3_170_SYMINT", NamedType.INT);
        stateInput.add("is_IsOverInfusionFlowRate_171_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsOverInfusionFlowRate_172_SYMINT", NamedType.INT);
        stateInput.add("is_InfusionNotStartedWarning_173_SYMINT", NamedType.INT);
        stateInput.add("is_active_InfusionNotStartedWarning_174_SYMINT", NamedType.INT);
        stateInput.add("is_IsOverInfusionVTBI_175_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsOverInfusionVTBI_176_SYMINT", NamedType.INT);
        stateInput.add("is_IsAirInLine_177_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsAirInLine_178_SYMINT", NamedType.INT);
        stateInput.add("is_IsOcclusion_179_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsOcclusion_180_SYMINT", NamedType.INT);
        stateInput.add("is_IsDoorOpen_181_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsDoorOpen_182_SYMINT", NamedType.INT);
        stateInput.add("is_active_Level2_183_SYMINT", NamedType.INT);
        stateInput.add("is_IsLowReservoir_184_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsLowReservoir_185_SYMINT", NamedType.INT);
        stateInput.add("is_active_Level1_186_SYMINT", NamedType.INT);
        stateInput.add("is_IsUnderInfusion_187_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsUnderInfusion_188_SYMINT", NamedType.INT);
        stateInput.add("is_IsFlowRateNotStable_189_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsFlowRateNotStable_190_SYMINT", NamedType.INT);
        stateInput.add("is_IsIdleTimeExceeded_191_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsIdleTimeExceeded_192_SYMINT", NamedType.INT);
        stateInput.add("is_IsPausedTimeExceeded_193_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsPausedTimeExceeded_194_SYMINT", NamedType.INT);
        stateInput.add("is_IsConfigTimeWarning_195_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsConfigTimeWarning_196_SYMINT", NamedType.INT);
        stateInput.add("is_IsBatteryError_197_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsBatteryError_198_SYMINT", NamedType.INT);
        stateInput.add("is_IsPumpHot_199_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsPumpHot_200_SYMINT", NamedType.INT);
        stateInput.add("is_IsLoggingFailed_201_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsLoggingFailed_202_SYMINT", NamedType.INT);
        stateInput.add("is_IsHardwareError_203_SYMINT", NamedType.INT);
        stateInput.add("is_active_IsHardwareError_204_SYMINT", NamedType.INT);
        stateInput.add("overInfusionTimer_205_SYMINT", NamedType.INT);
        stateInput.add("underInfusionTimer_206_SYMINT", NamedType.INT);
        stateInput.add("currentAlarm_207_SYMINT", NamedType.INT);
        stateInput.add("audioTimer_208_SYMINT", NamedType.INT);
        stateInput.add("cancelAlarm_209_SYMINT", NamedType.INT);
        stateInput.add("idletimer_210_SYMINT", NamedType.INT);
        stateInput.add("pausedtimer_211_SYMINT", NamedType.INT);

    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputGPCA() {

        //localB
        stateOutput.add(referenceObjectName_gpca_localB + ".Commanded_Flow_Rate.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Commanded_Flow_Rate.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Current_System_Mode.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Current_System_Mode.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Disable_Audio.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Disable_Audio.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".VTBI_High.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".VTBI_High.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Flow_Rate_High.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Flow_Rate_High.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Flow_Rate_Low.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Flow_Rate_Low.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Flow_Rate.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Flow_Rate.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Audio_Enable_Duration.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Audio_Enable_Duration.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Audio_Level.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Audio_Level.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Config_Warning_Duration.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Config_Warning_Duration.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Low_Reservoir.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Low_Reservoir.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Max_Duration_Over_Infusion.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Max_Duration_Over_Infusion.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Max_Duration_Under_Infusion.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Max_Duration_Under_Infusion.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Max_Paused_Duration.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Max_Paused_Duration.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Max_Idle_Duration.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Max_Idle_Duration.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Tolerance_Max.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Tolerance_Max.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Tolerance_Min.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Tolerance_Min.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Reservoir_Volume.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Reservoir_Volume.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Volume_Infused.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Volume_Infused.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".Config_Timer.1.3.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Config_Timer.1.3.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".ALARM_OUT_Display_Audio_Disabled_Indicator.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".ALARM_OUT_Display_Audio_Disabled_Indicator.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".ALARM_OUT_Display_Notification_Command.1.27.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".ALARM_OUT_Display_Notification_Command.1.27.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".ALARM_OUT_Audio_Notification_Command.1.63.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".ALARM_OUT_Audio_Notification_Command.1.63.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".ALARM_OUT_Highest_Level_Alarm.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".ALARM_OUT_Highest_Level_Alarm.1.13.51", new IntExpr(0));


        stateOutput.add(referenceObjectName_gpca_localB + ".ALARM_OUT_Log_Message_ID.1.15.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".ALARM_OUT_Log_Message_ID.1.15.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localB + ".System_On.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".System_On.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".System_Monitor_Failed.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".System_Monitor_Failed.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Logging_Failed.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Logging_Failed.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Infusion_Initiate.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Infusion_Initiate.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Notification_Cancel.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Notification_Cancel.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Flow_Rate_Not_Stable.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Flow_Rate_Not_Stable.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Air_In_Line.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Air_In_Line.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Occlusion.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Occlusion.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Door_Open.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Door_Open.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Temp.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Temp.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Air_Pressure.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Air_Pressure.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Humidity.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Humidity.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Battery_Depleted.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Battery_Depleted.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Battery_Low.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Battery_Low.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Battery_Unable_To_Charge.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Battery_Unable_To_Charge.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Supply_Voltage.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Supply_Voltage.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".CPU_In_Error.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".CPU_In_Error.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".RTC_In_Error.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".RTC_In_Error.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Watchdog_Interrupted.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Watchdog_Interrupted.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Memory_Corrupted.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Memory_Corrupted.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Pump_Too_Hot.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Pump_Too_Hot.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Pump_Overheated.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Pump_Overheated.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".Reservoir_Empty.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".Reservoir_Empty.1.3.51", new BoolExpr(false));

        stateOutput.add(referenceObjectName_gpca_localB + ".In_Therapy.1.3.51", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_gpca_localB + ".In_Therapy.1.3.51", new BoolExpr(false));


//localDW
        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_c2_ALARM_Functional.1.5.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_c2_ALARM_Functional.1.5.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_c2_ALARM_Functional.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_c2_ALARM_Functional.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Notification.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Notification.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_Visual.1.27.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_Visual.1.27.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Visual.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Visual.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_Audio.1.63.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_Audio.1.63.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Audio.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Audio.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_CheckAlarm.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_CheckAlarm.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_CancelAlarm.1.19.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_CancelAlarm.1.19.51", new IntExpr(0));


        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_CancelAlarm.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_CancelAlarm.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_SetAlarmStatus.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_SetAlarmStatus.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Level4.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Level4.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsEmptyReservoir.1.29.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsEmptyReservoir.1.29.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsEmptyReservoir.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsEmptyReservoir.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsSystemMonitorFailed.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsSystemMonitorFailed.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsSystemMonitorFailed.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsSystemMonitorFailed.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsEnviromentalError.1.29.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsEnviromentalError.1.29.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsEnviromentalError.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsEnviromentalError.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Level3.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Level3.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsOverInfusionFlowRate.1.43.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsOverInfusionFlowRate.1.43.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsOverInfusionFlowRate.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsOverInfusionFlowRate.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_InfusionNotStartedWarning.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_InfusionNotStartedWarning.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_InfusionNotStartedWarning.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_InfusionNotStartedWarning.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsOverInfusionVTBI.1.27.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsOverInfusionVTBI.1.27.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsOverInfusionVTBI.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsOverInfusionVTBI.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsAirInLine.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsAirInLine.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsAirInLine.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsAirInLine.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsOcclusion.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsOcclusion.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsOcclusion.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsOcclusion.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsDoorOpen.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsDoorOpen.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsDoorOpen.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsDoorOpen.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Level2.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Level2.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsLowReservoir.1.27.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsLowReservoir.1.27.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsLowReservoir.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsLowReservoir.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_Level1.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_Level1.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsUnderInfusion.1.37.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsUnderInfusion.1.37.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsUnderInfusion.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsUnderInfusion.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsFlowRateNotStable.1.27.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsFlowRateNotStable.1.27.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsFlowRateNotStable.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsFlowRateNotStable.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsIdleTimeExceeded.1.38.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsIdleTimeExceeded.1.38.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsIdleTimeExceeded.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsIdleTimeExceeded.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsPausedTimeExceeded.1.37.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsPausedTimeExceeded.1.37.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsPausedTimeExceeded.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsPausedTimeExceeded.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsConfigTimeWarning.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsConfigTimeWarning.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsConfigTimeWarning.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsConfigTimeWarning.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsBatteryError.1.29.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsBatteryError.1.29.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsBatteryError.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsBatteryError.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsPumpHot.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsPumpHot.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsPumpHot.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsPumpHot.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsLoggingFailed.1.25.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsLoggingFailed.1.25.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsLoggingFailed.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_active_IsLoggingFailed.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_IsHardwareError.1.35.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".is_IsHardwareError.1.35.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".is_active_IsHardwareError.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + "is_active_IsHardwareError.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".overInfusionTimer.1.23.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".overInfusionTimer.1.23.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".underInfusionTimer.1.23.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".underInfusionTimer.1.23.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".currentAlarm.1.13.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".currentAlarm.1.13.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".audioTimer.1.48.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".audioTimer.1.48.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".cancelAlarm.1.15.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".cancelAlarm.1.15.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".idletimer.1.40.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".idletimer.1.40.51", new IntExpr(0));

        stateOutput.add(referenceObjectName_gpca_localDW + ".pausedtimer.1.40.51", NamedType.INT);
        stateOutput.addInit(referenceObjectName_gpca_localDW + ".pausedtimer.1.40.51", new IntExpr(0));

    }


    //====================== INFUSION ====================================

    //entered by hand for now - this defines the output that we expect to validate with the T_node,i.e, this is the
    // output of the wrapper that gets plugged in the T_node to  validate it. Therefore it is not directly reflecting
    // the method output of the implementation, instead it is the output of the to-be-created r_wrapper node.


    private void discoverContractOutputInfusion() {

        contractOutput.add(referenceObjectName_infusion_Outputs + ".Commanded_Flow_Rate.1.3.67", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_infusion_Outputs + ".Commanded_Flow_Rate.1.3.67", new IntExpr(0));

        contractOutput.add(referenceObjectName_infusion_Outputs + ".Current_System_Mode.1.3.67", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_infusion_Outputs + ".Current_System_Mode.1.3.67", new IntExpr(0));

        contractOutput.add(referenceObjectName_infusion_Outputs + ".New_Infusion.1.3.67", NamedType.BOOL);
//        contractOutput.addInit(referenceObjectName_infusion_Outputs + ".New_Infusion.1.3.67", new BoolExpr(false));

        contractOutput.add(referenceObjectName_infusion_Outputs + ".Log_Message_ID.1.3.67", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_infusion_Outputs + ".Log_Message_ID.1.3.67", new IntExpr(0));

        contractOutput.add(referenceObjectName_infusion_Outputs + ".Actual_Infusion_Duration.1.3.67", NamedType.INT);
//        contractOutput.addInit(referenceObjectName_infusion_Outputs + ".Actual_Infusion_Duration.1.3.67", new IntExpr(0));

    }

    //entered by hand for now
    private void discoverFreeInputInfusion() {
        freeInput.add("System_On_1_SYMINT", NamedType.BOOL);
        /*freeInput.add("Request_Confirm_Stop_2_SYMINT", NamedType.BOOL);
        freeInput.add("Log_Message_ID1_3_SYMINT", NamedType.INT);
        freeInput.add("System_Start_4_SYMINT", NamedType.BOOL);
        freeInput.add("System_Stop_5_SYMINT", NamedType.BOOL);*/
        freeInput.add("Infusion_Initiate_6_SYMINT", NamedType.BOOL);
        freeInput.add("Infusion_Inhibit_7_SYMINT", NamedType.BOOL);
        freeInput.add("Infusion_Cancel_8_SYMINT", NamedType.BOOL);
        /*freeInput.add("Data_Config_9_SYMINT", NamedType.BOOL);
        freeInput.add("Next_10_SYMINT", NamedType.BOOL);
        freeInput.add("Back_11_SYMINT", NamedType.BOOL);
        freeInput.add("Cancel_12_SYMINT", NamedType.BOOL);
        freeInput.add("Keyboard_13_SYMINT", NamedType.BOOL);
        freeInput.add("Disable_Audio_14_SYMINT", NamedType.BOOL);
        freeInput.add("Notification_Cancel_15_SYMINT", NamedType.BOOL);
        freeInput.add("Configuration_Type_16_SYMINT", NamedType.INT);
        freeInput.add("Confirm_Stop_17_SYMINT", NamedType.BOOL);*/
        freeInput.add("Patient_Bolus_Request_18_SYMINT", NamedType.BOOL);
        /*freeInput.add("Patient_ID_19_SYMINT", NamedType.INT);
        freeInput.add("Drug_Name_20_SYMINT", NamedType.INT);
        freeInput.add("Drug_Concentration_21_SYMINT", NamedType.INT);*/
        freeInput.add("Infusion_Total_Duration_22_SYMINT", NamedType.INT);
        freeInput.add("VTBI_Total_23_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_Basal_24_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_Intermittent_Bolus_25_SYMINT", NamedType.INT);
        freeInput.add("Duration_Intermittent_Bolus_26_SYMINT", NamedType.INT);
        freeInput.add("Interval_Intermittent_Bolus_27_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_Patient_Bolus_28_SYMINT", NamedType.INT);
        freeInput.add("Duration_Patient_Bolus_29_SYMINT", NamedType.INT);
        freeInput.add("Lockout_Period_Patient_Bolus_30_SYMINT", NamedType.INT);
        freeInput.add("Max_Number_of_Patient_Bolus_31_SYMINT", NamedType.INT);
        freeInput.add("Flow_Rate_KVO_32_SYMINT", NamedType.INT);
        freeInput.add("Entered_Reservoir_Volume_33_SYMINT", NamedType.INT);
        /*freeInput.add("Reservoir_Volume_33_SYMINT", NamedType.INT);
         */
        freeInput.add("Configured_35_SYMINT", NamedType.INT);
        //freeInput.add("Error_Message_ID_36_SYMINT", NamedType.INT);
        /*freeInput.add("Request_Config_Type_36_SYMINT", NamedType.BOOL);
        freeInput.add("Request_Confirm_Infusion_Initiate_37_SYMINT", NamedType.BOOL);
        freeInput.add("Request_Patient_Drug_Info_38_SYMINT", NamedType.BOOL);
        freeInput.add("Request_Infusion_Info_39_SYMINT", NamedType.BOOL);
        freeInput.add("Log_Message_ID_40_SYMINT", NamedType.INT);
        freeInput.add("Config_Timer_41_SYMINT", NamedType.INT);
        freeInput.add("Config_Mode_42_SYMINT", NamedType.INT);
        freeInput.add("Is_Audio_Disabled_43_SYMINT", NamedType.INT);
        freeInput.add("Notification_Message_44_SYMINT", NamedType.INT);
        freeInput.add("Audio_Notification_Command_45_SYMINT", NamedType.INT);*/
        freeInput.add("Highest_Level_Alarm_47_SYMINT", NamedType.INT);
        //freeInput.add("Log_Message_ID3_47_SYMINT", NamedType.INT);
        freeInput.add("Reservoir_Empty_49_SYMINT", NamedType.BOOL);
        //freeInput.add("Reservoir_Volume2_49_SYMINT", NamedType.INT);
        freeInput.add("Volume_Infused_51_SYMINT", NamedType.INT);
        /*freeInput.add("Log_Message_ID2_51_SYMINT", NamedType.INT);
        freeInput.add("In_Therapy_52_SYMINT", NamedType.BOOL);*/

    }

    // IMP: order here is important. Firs the put the input variables for the state, then the state variables that account
// for the outputs of the sepc.
    //entered by hand for now
    private void discoverStateInputInfusion() {
        stateInput.add("Highest_Level_Alarm2_54_SYMINT", NamedType.INT);
        stateInput.add("Infusion_Total_Duration2_55_SYMINT", NamedType.INT);
        stateInput.add("VTBI_Total2_56_SYMINT", NamedType.INT);
        stateInput.add("Flow_Rate_Basal2_57_SYMINT", NamedType.INT);
        stateInput.add("Flow_Rate_Intermittent_Bolus2_58_SYMINT", NamedType.INT);
        stateInput.add("Duration_Intermittent_Bolus2_59_SYMINT", NamedType.INT);
        stateInput.add("Interval_Intermittent_Bolus2_60_SYMINT", NamedType.INT);
        stateInput.add("Flow_Rate_Patient_Bolus2_61_SYMINT", NamedType.INT);
        stateInput.add("Duration_Patient_Bolus2_62_SYMINT", NamedType.INT);
        stateInput.add("Lockout_Period_Patient_Bolus2_63_SYMINT", NamedType.INT);
        stateInput.add("Max_Number_of_Patient_Bolus2_64_SYMINT", NamedType.INT);
        stateInput.add("Flow_Rate_KVO2_65_SYMINT", NamedType.INT);
        stateInput.add("Configured2_66_SYMINT", NamedType.INT);
        stateInput.add("Volume_Infused2_67_SYMINT", NamedType.INT);
        stateInput.add("IM_OUT_Flow_Rate_Commanded2_68_SYMINT", NamedType.INT);
        stateInput.add("IM_OUT_Current_System_Mode2_69_SYMINT", NamedType.INT);
        stateInput.add("IM_OUT_Log_Message_ID2_70_SYMINT", NamedType.INT);
        stateInput.add("IM_OUT_Actual_Infusion_Duration2_71_SYMINT", NamedType.INT);
        stateInput.add("Infusion_Initiate2_72_SYMINT", NamedType.BOOL);
        stateInput.add("Infusion_Inhibit2_73_SYMINT", NamedType.BOOL);
        stateInput.add("Infusion_Cancel2_74_SYMINT", NamedType.BOOL);
        stateInput.add("Patient_Bolus_Request2_75_SYMINT", NamedType.BOOL);
        stateInput.add("Reservoir_Empty2_76_SYMINT", NamedType.BOOL);
        stateInput.add("IM_OUT_New_Infusion2_77_SYMINT", NamedType.BOOL);

        //localDW
        stateInput.add("is_active_c2_INFUSION_MGR_Functional_78_SYMINT", NamedType.INT);
        stateInput.add("is_c2_INFUSION_MGR_Functional_79_SYMINT", NamedType.INT);
        stateInput.add("is_Infusion_Manager_80_SYMINT", NamedType.INT);
        stateInput.add("is_THERAPY_81_SYMINT", NamedType.INT);
        stateInput.add("is_Arbiter_82_SYMINT", NamedType.INT);
        stateInput.add("is_active_Arbiter_83_SYMINT", NamedType.INT);
        stateInput.add("is_Alarm_Paused_84_SYMINT", NamedType.INT);
        stateInput.add("is_active_Alarm_Paused_85_SYMINT", NamedType.INT);
        stateInput.add("is_Manual_Paused_86_SYMINT", NamedType.INT);
        stateInput.add("is_active_Manual_Paused_87_SYMINT", NamedType.INT);
        stateInput.add("is_BASAL_88_SYMINT", NamedType.INT);
        stateInput.add("is_active_BASAL_89_SYMINT", NamedType.INT);
        stateInput.add("is_Arbiter_d_90_SYMINT", NamedType.INT);
        stateInput.add("is_active_Arbiter_c_91_SYMINT", NamedType.INT);
        stateInput.add("is_PATIENT_92_SYMINT", NamedType.INT);
        stateInput.add("is_active_PATIENT_93_SYMINT", NamedType.INT);
        stateInput.add("is_INTERMITTENT_94_SYMINT", NamedType.INT);
        stateInput.add("is_active_INTERMITTENT_95_SYMINT", NamedType.INT);
        stateInput.add("sbolus_timer_96_SYMINT", NamedType.INT);
        stateInput.add("pbolus_timer_97_SYMINT", NamedType.INT);
        stateInput.add("lock_timer_98_SYMINT", NamedType.INT);
        stateInput.add("number_pbolus_99_SYMINT", NamedType.INT);
        stateInput.add("sbolusInter_timer_100_SYMINT", NamedType.INT);
        stateInput.add("sbolus_req_101_SYMINT", NamedType.BOOL);
        stateInput.add("inPatientBolus_102_SYMINT", NamedType.BOOL);

    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputInfusion() {

        stateOutput.add(referenceObjectName_infusion_localB + ".Highest_Level_Alarm.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Highest_Level_Alarm.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Infusion_Total_Duration.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Infusion_Total_Duration.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".VTBI_Total.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".VTBI_Total.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Flow_Rate_Basal.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Flow_Rate_Basal.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Flow_Rate_Intermittent_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Flow_Rate_Intermittent_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Duration_Intermittent_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Duration_Intermittent_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Interval_Intermittent_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Interval_Intermittent_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Flow_Rate_Patient_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Flow_Rate_Patient_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Duration_Patient_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Duration_Patient_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Lockout_Period_Patient_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Lockout_Period_Patient_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Max_Number_of_Patient_Bolus.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Max_Number_of_Patient_Bolus.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Flow_Rate_KVO.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Flow_Rate_KVO.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Configured.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Configured.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Volume_Infused.1.3.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Volume_Infused.1.3.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".IM_OUT_Flow_Rate_Commanded.1.159.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".IM_OUT_Flow_Rate_Commanded.1.159.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".IM_OUT_Current_System_Mode.1.148.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".IM_OUT_Current_System_Mode.1.148.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".IM_OUT_Log_Message_ID.1.29.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".IM_OUT_Log_Message_ID.1.29.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".IM_OUT_Actual_Infusion_Duration.1.37.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localB + "IM_OUT_Actual_Infusion_Duration.1.37.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localB + ".Infusion_Initiate.1.3.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Infusion_Initiate.1.3.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localB + ".Infusion_Inhibit.1.3.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Infusion_Inhibit.1.3.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localB + ".Infusion_Cancel.1.3.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Infusion_Cancel.1.3.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localB + ".Patient_Bolus_Request.1.3.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Patient_Bolus_Request.1.3.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localB + ".Reservoir_Empty.1.3.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".Reservoir_Empty.1.3.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localB + ".IM_OUT_New_Infusion.1.26.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localB + ".IM_OUT_New_Infusion.1.26.67", new BoolExpr(false));

        //localDW
        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_c2_INFUSION_MGR_Functional.1.5.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_c2_INFUSION_MGR_Functional.1.5.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_c2_INFUSION_MGR_Functional.1.13.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_c2_INFUSION_MGR_Functional.1.13.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_Infusion_Manager.1.29.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_Infusion_Manager.1.29.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_THERAPY.1.52.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_THERAPY.1.52.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_Arbiter.1.109.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_Arbiter.1.109.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_Arbiter.1.51.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_Arbiter.1.51.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_Alarm_Paused.1.65.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_Alarm_Paused.1.65.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_Alarm_Paused.1.51.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_Alarm_Paused.1.51.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_Manual_Paused.1.65.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_Manual_Paused.1.65.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_Manual_Paused.1.51.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_Manual_Paused.1.51.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_BASAL.1.49.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_BASAL.1.49.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_BASAL.1.43.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + "is_active_BASAL.1.43.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_Arbiter_d.1.71.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_Arbiter_d.1.71.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_Arbiter_c.1.43.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_Arbiter_c.1.43.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_PATIENT.1.73.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_PATIENT.1.73.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_PATIENT.1.43.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_PATIENT.1.43.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_INTERMITTENT.1.59.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_INTERMITTENT.1.59.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".is_active_INTERMITTENT.1.43.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".is_active_INTERMITTENT.1.43.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".sbolus_timer.1.64.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".sbolus_timer.1.64.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".pbolus_timer.1.78.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".pbolus_timer.1.78.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".lock_timer.1.53.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".lock_timer.1.53.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".number_pbolus.1.52.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".number_pbolus.1.52.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".sbolusInter_timer.1.62.67", NamedType.INT);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".sbolusInter_timer.1.62.67", new IntExpr(0));

        stateOutput.add(referenceObjectName_infusion_localDW + ".sbolus_req.1.63.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".sbolus_req.1.63.67", new BoolExpr(false));

        stateOutput.add(referenceObjectName_infusion_localDW + ".inPatientBolus.1.56.67", NamedType.BOOL);
        stateOutput.addInit(referenceObjectName_infusion_localDW + ".inPatientBolus.1.56.67", new BoolExpr(false));
    }

    //=========================== Even ============================
    //entered by hand for now

    private void discoverContractOutputEven() {
        contractOutput.add(referenceObjectName + ".output.1.5.2", NamedType.INT);
//        contractOutput.addInit(referenceObjectName + ".output.1.5.2", new IntExpr(8));
    }

    //entered by hand for now
    private void discoverFreeInputEven() {
        freeInput.add("signal", NamedType.BOOL);
    }

    //entered by hand for now
    private void discoverStateInputEven() {
        stateInput.add("countState", NamedType.INT);
        stateInput.add("output", NamedType.INT);
    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputEven() {
        stateOutput.add(referenceObjectName + ".countState.1.5.2", NamedType.INT);
        stateOutput.addInit(referenceObjectName + ".countState.1.5.2", new IntExpr(0));

    }


    //=========================== Vote ===========================

    private void discoverContractOutputVote() {

        contractOutput.add(referenceObjectName + ".out.1.7.4", NamedType.BOOL);
        //contractOutput.addInit(referenceObjectName + ".out.1.9.4", new BoolExpr(false));

    }

    //entered by hand for now
    private void discoverFreeInputVote() {
        freeInput.add("a_1_SYMINT", NamedType.BOOL);
        freeInput.add("b_2_SYMINT", NamedType.BOOL);
        freeInput.add("c_3_SYMINT", NamedType.BOOL);

    }

    //entered by hand for now
    private void discoverStateInputVote() {
        stateInput.add("counter_5_SYMINT", NamedType.INT);
        //stateInput.add("out_6_SYMINT", NamedType.BOOL);
    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputVote() {
        stateOutput.add(referenceObjectName + ".counter.1.3.4", NamedType.INT);
        stateOutput.addInit(referenceObjectName + ".counter.1.3.4", new IntExpr(0));
    }


    //=========================== Vote2 ===========================

    private void discoverContractOutputVote2() {

        contractOutput.add(referenceObjectName + "r347.out.1.1.2", NamedType.BOOL);
        contractOutput.addInit(referenceObjectName + "r347.out.1.1.2", new BoolExpr(false));
    }

    //entered by hand for now
    private void discoverFreeInputVote2() {
        freeInput.add("a", NamedType.INT);
        freeInput.add("b", NamedType.INT);
        freeInput.add("c", NamedType.INT);
        freeInput.add("threshold", NamedType.INT);
    }

    //entered by hand for now
    private void discoverStateInputVote2() {
        stateInput.add("out", NamedType.BOOL);
    }

    //entered by hand for now - order is important, needs to match in order of the input
    private void discoverStateOutputVote2() {
    }


    public ArrayList<VarDecl> generateInputDecl() {
        ArrayList<VarDecl> inputDeclList = generateFreeInputDecl();
        inputDeclList.addAll(generateStateInputDecl());
        return inputDeclList;
    }

    public ArrayList<VarDecl> generateFreeInputDecl() {
        return generateLustreDecl(freeInput);
    }

    public ArrayList<VarDecl> generateStateInputDecl() {
        return generateLustreDecl(stateInput);
    }

    private ArrayList<VarDecl> generateLustreDecl(SpecInputOutput inputOutput) {
        return inputOutput.generateVarDecl();
    }

    public ArrayList<VarDecl> generaterContractOutDeclList() {
        return contractOutput.generateVarDecl();
    }

    public ArrayList<VarDecl> generateOutputDecl() {
        return stateOutput.generateVarDecl();
    }

    /**
     * searches in all in input and output arrays to check if it is one in them
     *
     * @param s
     * @return
     */
    public boolean isInOutVar(String s, NamedType type) {
        return isFreeInVar(s, type) || isStateInVar(s, type) || isStateOutVar(s, type) || isContractOutputVar(s, type);
    }


    public boolean isFreeInVar(String varName, NamedType type) {
        return freeInput.contains(varName, type);
    }

    public boolean isStateInVar(String varName, NamedType type) {
        return stateInput.contains(varName, type);
    }

    public boolean isStateOutVar(String varName, NamedType type) {
        return stateOutput.contains(varName, type);
    }

    public boolean isContractOutputVar(String varName, NamedType type) {
        return contractOutput.contains(varName, type);
    }


    public boolean isStateOutVar(String name) {
        return stateOutput.hasName(name);
    }

    /*public Pair<VarDecl, Equation> replicateContractOutput(String outVarName) {
        return contractOutput.replicateMe(outVarName);
    }
*/
    /*public NamedType getContractOutType() {
        if (contractOutput.varList.size() == 0) {
            System.out.println("Contract has no output, this is unexpected signature for contract R! Aborting!");
            assert false;
        }
        return contractOutput.varList.get(0).getSecond();
    }
*/
    //gets the initial value of a wrapper output.
    /*public Expr getContractOutputInit(String name) {
        return contractOutput.getReturnInitVal(name);
    }*/

    public Expr getStateOutInit(String name) {
        return stateOutput.getReturnInitVal(name);
    }

    public Expr getStateOutInit(int index) {
        return stateOutput.getReturnInitVal(index);
    }

    public ContractOutput getContractOutput() {
        return contractOutput;
    }

    public String varOutNameByIndex(int index) {
        return contractOutput.varNameForIndex(index);
    }

    public String varInputNameByIndex(int index) {
        return freeInput.varNameForIndex(index);
    }
}
