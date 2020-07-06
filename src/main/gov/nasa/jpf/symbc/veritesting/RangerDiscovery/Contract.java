package gov.nasa.jpf.symbc.veritesting.RangerDiscovery;

import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput.InOutManager;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput.SpecInOutManager;
import jkind.lustre.NamedType;

import static gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract.contractDiscoveryOn;

public class Contract {

    public final InOutManager rInOutManager = new InOutManager();
    public final SpecInOutManager tInOutManager = new SpecInOutManager();

    Contract(){
        tInOutManager.discoverVars();
        rInOutManager.discoverVars(tInOutManager);
    }

    /**
     * This method is used to collect the input of a method, later for contract discovery or for lustre translation.
     * currently unused
     */
    public void collectInput(){
        if(!contractDiscoveryOn){
            System.out.println("collectInput is valid only when contractDiscovery is turned on");
            assert false;
        }
    }

    public String specToImplementationVar(String varName){
        int varIndexInSpec  = tInOutManager.indexOfInputVar(varName);
        String varNameInImpl = rInOutManager.varInputNameByIndex(varIndexInSpec);

        return varNameInImpl;
    }

}
