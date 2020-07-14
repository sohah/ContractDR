package DiscoveryExamples.System_Monitor;/*
 * Code generation for system model 'SYS_STATS_Functional'
 *
 * Model                      : SYS_STATS_Functional
 * Model version              : 1.1404
 * Simulink Coder version : 8.4 (R2013a) 13-Feb-2013
 * C source code generated on : Thu Sep 12 09:50:15 2013
 *
 * Note that the functions contained in this file are part of a Simulink
 * model, and are not self-contained algorithms.
 */

import DiscoveryExamples.gpca.*;

public class SYS_STATS_Functional_Recovered {
    /* Initial conditions for referenced model: 'SYS_STATS_Functional' */
    static void SYS_STATS_Functional_Init(DW_SYS_STATS_Functional_f_T localDW) {
        /* InitializeConditions for UnitDelay: '<S5>/Unit Delay' */
        localDW.UnitDelay_DSTATE = 0;
    }

    /* Outputs for referenced model: 'SYS_STATS_Functional' */
    static void SYS_STATS_Functional(Infusion_Manager_Outputs rtu_IM_IN,
                                     Top_Level_Mode_Outputs rtu_TLM_MODE_IN,
                                     Device_Sensor_Inputs rtu_SENSOR_IN,
                                     Device_Configuration_Inputs rtu_CONST_IN,
                                     Config_Outputs rtu_CONFIG_IN, System_Status_Outputs rty_SYS_STAT_OUT,
                                     B_SYS_STATS_Functional_c_T localB, DW_SYS_STATS_Functional_f_T localDW) {
        int rtb_Reservoir_Volume_e;
        int tmp;

        /* Switch: '<S4>/Reservoir_Volume' incorporates:
         *  Constant: '<S4>/Constant'
         *  Product: '<S4>/Divide'
         *  Sum: '<S4>/Subtract'
         *  Switch: '<S4>/Reservoir_Empty'
         */
//     printf("\n\r1: %d %d %d %d %d\n\r",rty_SYS_STAT_OUT->Reservoir_Empty,rtb_Reservoir_Volume_e,
//     	rtu_CONST_IN->Empty_Reservoir,rtu_CONFIG_IN->Reservoir_Volume,rtu_SENSOR_IN->Flow_Rate);
        if (rtu_TLM_MODE_IN.System_On) {
            rtb_Reservoir_Volume_e = (rtu_CONFIG_IN.Reservoir_Volume -
                    rtu_SENSOR_IN.Flow_Rate);

            /* Switch: '<S4>/if then ' incorporates:
             *  Product: '<S4>/Divide'
             *  RelationalOperator: '<S4>/Relational Operator'
             *  Sum: '<S4>/Subtract'
             */
//     printf("2: %d %d %d\n\r",rty_SYS_STAT_OUT->Reservoir_Empty,rtb_Reservoir_Volume_e,rtu_CONST_IN->Empty_Reservoir);
            rty_SYS_STAT_OUT.Reservoir_Empty = (rtb_Reservoir_Volume_e <
                    rtu_CONST_IN.Empty_Reservoir);
        } else {
            rtb_Reservoir_Volume_e = 0;

            /* Switch: '<S4>/if then ' incorporates:
             *  Constant: '<S4>/Constant'
             *  Constant: '<S4>/Constant16'
             */
            rty_SYS_STAT_OUT.Reservoir_Empty = false;
        }
//     printf("3: %d %d %d\n\r",rty_SYS_STAT_OUT->Reservoir_Empty,rtb_Reservoir_Volume_e,rtu_CONST_IN->Empty_Reservoir);

        /* End of Switch: '<S4>/Reservoir_Volume' */

        /* Switch: '<S5>/if then 4 ' incorporates:
         *  Constant: '<S5>/Constant8'
         *  Logic: '<S5>/Logical Operator3'
         *  UnitDelay: '<S5>/Unit Delay'
         */
        if (!rtu_IM_IN.New_Infusion) {
            tmp = localDW.UnitDelay_DSTATE;
        } else {
            tmp = 0;
        }

        /* End of Switch: '<S5>/if then 4 ' */

        /* Sum: '<S5>/Subtract1' incorporates:
         *  Product: '<S5>/Divide1'
         */
        localB.Subtract1 = (tmp + rtu_SENSOR_IN.Flow_Rate);
//     printf("10: %d %d %d %d\n\r",localB->Subtract1,tmp,rtu_SENSOR_IN->Flow_Rate,localDW->UnitDelay_DSTATE);

        /* Switch: '<S3>/Total_Volume_Infused2' incorporates:
         *  Constant: '<S3>/Constant20'
         *  Constant: '<S3>/Constant21'
         *  Switch: '<S2>/In_Therapy'
         */
        if (rtu_TLM_MODE_IN.System_On) {
            rty_SYS_STAT_OUT.Log_Message_ID = 8;

            /* Switch: '<S2>/if then6' incorporates:
             *  Constant: '<S2>/Constant5'
             *  Constant: '<S3>/Constant21'
             *  RelationalOperator: '<S2>/Relational Operator1'
             */
            rty_SYS_STAT_OUT.In_Therapy = (rtu_IM_IN.Current_System_Mode > 1);
        } else {
            rty_SYS_STAT_OUT.Log_Message_ID = 0;

            /* Switch: '<S2>/if then6' incorporates:
             *  Constant: '<S2>/Constant17'
             *  Constant: '<S3>/Constant20'
             */
            rty_SYS_STAT_OUT.In_Therapy = false;
        }

        /* End of Switch: '<S3>/Total_Volume_Infused2' */

        /* BusCreator: '<Root>/BusConversion_InsertedFor_SYS_STAT_OUT_at_inport_0' */
        rty_SYS_STAT_OUT.Reservoir_Volume = rtb_Reservoir_Volume_e;

        /* Switch: '<S5>/Total_Volume_Infused' */
        if (rtu_TLM_MODE_IN.System_On) {
            /* BusCreator: '<Root>/BusConversion_InsertedFor_SYS_STAT_OUT_at_inport_0' */
            rty_SYS_STAT_OUT.Volume_Infused = localB.Subtract1;
        } else {
            /* BusCreator: '<Root>/BusConversion_InsertedFor_SYS_STAT_OUT_at_inport_0' incorporates:
             *  Constant: '<S5>/Constant9'
             */
            rty_SYS_STAT_OUT.Volume_Infused = 0;
        }

        /* End of Switch: '<S5>/Total_Volume_Infused' */
    }

    /* Update for referenced model: 'SYS_STATS_Functional' */
    public void SYS_STATS_Functional_Update(B_SYS_STATS_Functional_c_T localB,
                                            DW_SYS_STATS_Functional_f_T localDW) {
        /* Update for UnitDelay: '<S5>/Unit Delay' */
        localDW.UnitDelay_DSTATE = localB.Subtract1;
    }


    public static void main(String[] args) {

        SYS_STATS_FunctionalSymWrapper(1, 1, false, 1, 1, false, false, 1, 1, false,
                false, false, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, false, false, false, false, 1, 1, 1, false,
                1, 1, 1, false);
    }

    static private void SYS_STATS_FunctionalSymWrapper(//Symbolic input of Infusion_Manager_Outputs
                                                       int Commanded_Flow_Rate,
                                                       int Current_System_Mode,
                                                       boolean New_Infusion,
                                                       int Log_Message_ID_1,
                                                       int Actual_Infusion_Duration,


                                                       //Symbolic input of Top_Level_Mode_Outputs
                                                       boolean System_On,
                                                       boolean Request_Confirm_Stop,
                                                       int Log_Message_ID_2,


                                                       //Symbolic input of Device_Sensor_Inputs
                                                       int Flow_Rate,
                                                       boolean Flow_Rate_Not_Stable,
                                                       boolean Air_In_Line,
                                                       boolean Occlusion,
                                                       boolean Door_Open,
                                                       boolean Temp,
                                                       boolean Air_Pressure,
                                                       boolean Humidity,
                                                       boolean Battery_Depleted,
                                                       boolean Battery_Low,
                                                       boolean Battery_Unable_To_Charge,
                                                       boolean Supply_Voltage,
                                                       boolean CPU_In_Error,
                                                       boolean RTC_In_Error,
                                                       boolean Watchdog_Interrupted,
                                                       boolean Memory_Corrupted,
                                                       boolean Pump_Too_Hot,
                                                       boolean Pump_Overheated,
                                                       boolean Pump_Primed,
                                                       boolean Post_Successful,

                                                       //Symbolic input of Device_Configuration_Inputs
                                                       int Audio_Enable_Duration,
                                                       int Audio_Level,
                                                       int Config_Warning_Duration,
                                                       int Empty_Reservoir,
                                                       int Low_Reservoir,
                                                       int Max_Config_Duration,
                                                       int Max_Duration_Over_Infusion,
                                                       int Max_Duration_Under_Infusion,
                                                       int Max_Paused_Duration,
                                                       int Max_Idle_Duration,
                                                       int Tolerance_Max,
                                                       int Tolerance_Min,
                                                       int Log_Interval,
                                                       int System_Test_Interval,
                                                       int Max_Display_Duration,
                                                       int Max_Confirm_Stop_Duration,


                                                       //Symbolic input of Config_Outputs
                                                       int Patient_ID,
                                                       int Drug_Name2,
                                                       int Drug_Concentration,
                                                       int Infusion_Total_Duration,
                                                       int VTBI_Total,
                                                       int Flow_Rate_Basal,
                                                       int Flow_Rate_Intermittent_Bolus,
                                                       int Duration_Intermittent_Bolus,
                                                       int Interval_Intermittent_Bolus,
                                                       int Flow_Rate_Patient_Bolus,
                                                       int Duration_Patient_Bolus,
                                                       int Lockout_Period_Patient_Bolus,
                                                       int Max_Number_of_Patient_Bolus,
                                                       int Flow_Rate_KVO2,
                                                       int Entered_Reservoir_Volume,
                                                       int Reservoir_Volume2,
                                                       int Configured,
                                                       int Error_Message_ID,
                                                       boolean Request_Config_Type,
                                                       boolean Request_Confirm_Infusion_Initiate,
                                                       boolean Request_Patient_Drug_Info,
                                                       boolean Request_Infusion_Info,
                                                       int Log_Message_ID4,
                                                       int Config_Timer,
                                                       int Config_Mode,

                                                       //Symbolic input of System_Status_Outputs
                                                       boolean Reservoir_Empty,
                                                       int Reservoir_Volume,
                                                       int Volume_Infused,
                                                       int Log_Message_ID,
                                                       boolean In_Therapy)

    //,B_ALARM_Functional_c_T localB, DW_ALARM_Functional_f_T localDW)
    {

        DiscoveryExamples.System_Monitor.Infusion_Manager_Outputs rtu_IM_IN = new DiscoveryExamples.System_Monitor.Infusion_Manager_Outputs();
        /*rtu_IM_IN.Commanded_Flow_Rate = Commanded_Flow_Rate;
        rtu_IM_IN.Current_System_Mode = Current_System_Mode;
        rtu_IM_IN.New_Infusion = New_Infusion;
        rtu_IM_IN.Log_Message_ID = Log_Message_ID_1;
        rtu_IM_IN.Actual_Infusion_Duration = Actual_Infusion_Duration;*/

        DiscoveryExamples.System_Monitor.Top_Level_Mode_Outputs rtu_tlm_mode_in = new DiscoveryExamples.System_Monitor.Top_Level_Mode_Outputs();
        /*rtu_tlm_mode_in.System_On = System_On;
        rtu_tlm_mode_in.Request_Confirm_Stop = Request_Confirm_Stop;
        rtu_tlm_mode_in.Log_Message_ID = Log_Message_ID_2;
*/
        DiscoveryExamples.System_Monitor.Device_Sensor_Inputs rtu_sensor_in = new DiscoveryExamples.System_Monitor.Device_Sensor_Inputs();
  /*      rtu_sensor_in.Flow_Rate = Flow_Rate;
        rtu_sensor_in.Flow_Rate_Not_Stable = Flow_Rate_Not_Stable;
        rtu_sensor_in.Air_In_Line = Air_In_Line;
        rtu_sensor_in.Occlusion = Occlusion;
        rtu_sensor_in.Door_Open = Door_Open;
        rtu_sensor_in.Temp = Temp;
        rtu_sensor_in.Air_Pressure = Air_Pressure;
        rtu_sensor_in.Humidity = Humidity;
        rtu_sensor_in.Battery_Depleted = Battery_Depleted;
        rtu_sensor_in.Battery_Low = Battery_Low;
        rtu_sensor_in.Battery_Unable_To_Charge = Battery_Unable_To_Charge;
        rtu_sensor_in.Supply_Voltage = Supply_Voltage;
        rtu_sensor_in.CPU_In_Error = CPU_In_Error;
        rtu_sensor_in.RTC_In_Error = RTC_In_Error;
        rtu_sensor_in.Watchdog_Interrupted = Watchdog_Interrupted;
        rtu_sensor_in.Memory_Corrupted = Memory_Corrupted;
        rtu_sensor_in.Pump_Too_Hot = Pump_Too_Hot;
        rtu_sensor_in.Pump_Overheated = Pump_Overheated;
        rtu_sensor_in.Pump_Primed = Pump_Primed;
        rtu_sensor_in.Post_Successful = Post_Successful;
*/
        DiscoveryExamples.System_Monitor.Device_Configuration_Inputs rtu_const_in = new DiscoveryExamples.System_Monitor.Device_Configuration_Inputs();
/*
        rtu_const_in.Audio_Enable_Duration = Audio_Enable_Duration;
        rtu_const_in.Audio_Level = Audio_Level;
        rtu_const_in.Config_Warning_Duration = Config_Warning_Duration;
        rtu_const_in.Empty_Reservoir = Empty_Reservoir;
        rtu_const_in.Low_Reservoir = Low_Reservoir;
        rtu_const_in.Max_Config_Duration = Max_Config_Duration;
        rtu_const_in.Max_Duration_Over_Infusion = Max_Duration_Over_Infusion;
        rtu_const_in.Max_Duration_Under_Infusion = Max_Duration_Under_Infusion;
        rtu_const_in.Max_Paused_Duration = Max_Paused_Duration;
        rtu_const_in.Max_Idle_Duration = Max_Idle_Duration;
        rtu_const_in.Tolerance_Max = Tolerance_Max;
        rtu_const_in.Tolerance_Min = Tolerance_Min;
        rtu_const_in.Log_Interval = Log_Interval;
        rtu_const_in.System_Test_Interval = System_Test_Interval;
        rtu_const_in.Max_Display_Duration = Max_Display_Duration;
        rtu_const_in.Max_Confirm_Stop_Duration = Max_Confirm_Stop_Duration;
*/

        DiscoveryExamples.System_Monitor.System_Status_Outputs rtu_sys_stat_in = new DiscoveryExamples.System_Monitor.System_Status_Outputs();
  /*      rtu_sys_stat_in.Reservoir_Empty = Reservoir_Empty;
        rtu_sys_stat_in.Reservoir_Volume = Reservoir_Volume;
        rtu_sys_stat_in.Volume_Infused = Volume_Infused;
        rtu_sys_stat_in.Log_Message_ID = Log_Message_ID;
        rtu_sys_stat_in.In_Therapy = In_Therapy;
*/
        DiscoveryExamples.System_Monitor.Config_Outputs rtu_config_in = new DiscoveryExamples.System_Monitor.Config_Outputs();
        rtu_config_in.Patient_ID = Patient_ID;
        rtu_config_in.Drug_Name = Drug_Name2;
        rtu_config_in.Drug_Concentration = Drug_Concentration;
        rtu_config_in.Infusion_Total_Duration = Infusion_Total_Duration;
        rtu_config_in.VTBI_Total = VTBI_Total;
        rtu_config_in.Flow_Rate_Basal = Flow_Rate_Basal;
        rtu_config_in.Flow_Rate_Intermittent_Bolus = Flow_Rate_Intermittent_Bolus;
        rtu_config_in.Duration_Intermittent_Bolus = Duration_Intermittent_Bolus;
        rtu_config_in.Interval_Intermittent_Bolus = Interval_Intermittent_Bolus;
        rtu_config_in.Flow_Rate_Patient_Bolus = Flow_Rate_Patient_Bolus;
        rtu_config_in.Duration_Patient_Bolus = Duration_Patient_Bolus;
        rtu_config_in.Lockout_Period_Patient_Bolus = Lockout_Period_Patient_Bolus;
        rtu_config_in.Max_Number_of_Patient_Bolus = Max_Number_of_Patient_Bolus;
        rtu_config_in.Flow_Rate_KVO = Flow_Rate_KVO2;
        rtu_config_in.Entered_Reservoir_Volume = Entered_Reservoir_Volume;
        rtu_config_in.Reservoir_Volume = Reservoir_Volume2;
        rtu_config_in.Configured = Configured;
        rtu_config_in.Error_Message_ID = Error_Message_ID;
        rtu_config_in.Request_Config_Type = Request_Config_Type;
        rtu_config_in.Request_Confirm_Infusion_Initiate = Request_Confirm_Infusion_Initiate;
        rtu_config_in.Request_Patient_Drug_Info = Request_Patient_Drug_Info;
        rtu_config_in.Request_Infusion_Info = Request_Infusion_Info;
        rtu_config_in.Log_Message_ID = Log_Message_ID4;
        rtu_config_in.Config_Timer = Config_Timer;
        rtu_config_in.Config_Mode = Config_Mode;


        System_Status_Outputs rty_SYS_STAT_OUT = new System_Status_Outputs();
        rty_SYS_STAT_OUT.Reservoir_Empty = Reservoir_Empty;
        rty_SYS_STAT_OUT.Reservoir_Volume = Reservoir_Volume;
        rty_SYS_STAT_OUT.Volume_Infused = Volume_Infused;
        rty_SYS_STAT_OUT.Log_Message_ID = Log_Message_ID;
        rty_SYS_STAT_OUT.In_Therapy = In_Therapy;

		B_SYS_STATS_Functional_c_T localB = new B_SYS_STATS_Functional_c_T();
		DW_SYS_STATS_Functional_f_T localDW = new DW_SYS_STATS_Functional_f_T();
        if ((0 <= Commanded_Flow_Rate) &&
                (0 <= Current_System_Mode) &&
                (0 <= Log_Message_ID_1) &&
                (0 <= Actual_Infusion_Duration) &&
                (0 <= Log_Message_ID_2) &&
                (0 <= Flow_Rate) &&
                (0 <= Audio_Enable_Duration) &&
                (0 <= Audio_Level) &&
                (0 <= Config_Warning_Duration) &&
                (0 <= Empty_Reservoir) &&
                (0 <= Low_Reservoir) &&
                (0 <= Max_Config_Duration) &&
                (0 <= Max_Duration_Over_Infusion) &&
                (0 <= Max_Duration_Under_Infusion) &&
                (0 <= Max_Paused_Duration) &&
                (0 <= Max_Idle_Duration) &&
                (0 <= Tolerance_Max) &&
                (0 <= Tolerance_Min) &&
                (0 <= Log_Interval) &&
                (0 <= System_Test_Interval) &&
                (0 <= Max_Display_Duration) &&
                (0 <= Max_Confirm_Stop_Duration) &&
                (0 <= Patient_ID) &&
                (0 <= Drug_Name2) &&
                (0 <= Drug_Concentration) &&
                (0 <= Infusion_Total_Duration) &&
                (0 <= VTBI_Total) &&
                (0 <= Flow_Rate_Basal) &&
                (0 <= Flow_Rate_Intermittent_Bolus) &&
                (0 <= Duration_Intermittent_Bolus) &&
                (0 <= Interval_Intermittent_Bolus) &&
                (0 <= Flow_Rate_Patient_Bolus) &&
                (0 <= Duration_Patient_Bolus) &&
                (0 <= Lockout_Period_Patient_Bolus) &&
                (0 <= Max_Number_of_Patient_Bolus) &&
                (0 <= Flow_Rate_KVO2) &&
                (0 <= Entered_Reservoir_Volume) &&
                (0 <= Reservoir_Volume2) &&
                (0 <= Configured) &&
                (0 <= Error_Message_ID) &&
                (0 <= Log_Message_ID4) &&
                (0 <= Config_Timer) &&
                (0 <= Config_Mode) &&
				(0 <= Reservoir_Volume) &&
				(0 <= Volume_Infused) &&
				(0 <= Log_Message_ID) &&
                (Commanded_Flow_Rate <= 255) &&
                (Current_System_Mode <= 255) &&
                (Log_Message_ID_1 <= 255) &&
                (Actual_Infusion_Duration <= 255) &&
                (Log_Message_ID_2 <= 255) &&
                (Flow_Rate <= 255) &&
                (Audio_Enable_Duration <= 255) &&
                (Audio_Level <= 255) &&
                (Config_Warning_Duration <= 255) &&
                (Empty_Reservoir <= 255) &&
                (Low_Reservoir <= 255) &&
                (Max_Config_Duration <= 255) &&
                (Max_Duration_Over_Infusion <= 255) &&
                (Max_Duration_Under_Infusion <= 255) &&
                (Max_Paused_Duration <= 255) &&
                (Max_Idle_Duration <= 255) &&
                (Tolerance_Max <= 255) &&
                (Tolerance_Min <= 255) &&
                (Log_Interval <= 255) &&
                (System_Test_Interval <= 255) &&
                (Max_Display_Duration <= 255) &&
                (Max_Confirm_Stop_Duration <= 255) &&
                (Patient_ID <= 255) &&
                (Drug_Name2 <= 255) &&
                (Drug_Concentration <= 255) &&
                (Infusion_Total_Duration <= 255) &&
                (VTBI_Total <= 255) &&
                (Flow_Rate_Basal <= 255) &&
                (Flow_Rate_Intermittent_Bolus <= 255) &&
                (Duration_Intermittent_Bolus <= 255) &&
                (Interval_Intermittent_Bolus <= 255) &&
                (Flow_Rate_Patient_Bolus <= 255) &&
                (Duration_Patient_Bolus <= 255) &&
                (Lockout_Period_Patient_Bolus <= 255) &&
                (Max_Number_of_Patient_Bolus <= 255) &&
                (Flow_Rate_KVO2 <= 255) &&
                (Entered_Reservoir_Volume <= 255) &&
                (Reservoir_Volume2 <= 255) &&
                (Configured <= 255) &&
                (Error_Message_ID <= 255) &&
                (Log_Message_ID4 <= 255) &&
                (Config_Timer <= 255) &&
                (Config_Mode <= 255) &&
				(Reservoir_Volume <=255) &&
				(Volume_Infused <= 255) &&
				(Log_Message_ID <= 255)

        ) {
			SYS_STATS_Functional_Init(localDW);

			SYS_STATS_Functional(rtu_IM_IN, rtu_tlm_mode_in, rtu_sensor_in, rtu_const_in, rtu_config_in, rty_SYS_STAT_OUT, localB, localDW);
        }
    }

}