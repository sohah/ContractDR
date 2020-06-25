public class TestCasePerf {

    public static void main(String[] args) {
        /*A myA = new A();
        int myVal = myA.getIncA();
        System.out.println("my A value is = " + myVal);*/
//        singleBranchCov(1, 1);
//        doubleBranchCov(1, 1);
//        doubleRec(1, 1);
//        complexBranchCov(1,1);
//        unoptimalDFS(1, 1);
//        doubleLoopUnoptimalDFS(1, 1);
//        doubleLoop(1, 1);

        (new TestCasePerf()).testingSymbolicFields(1,1);
    }

    public static int singleBranchCov(int x, int y) {

        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y;
            else x = x + 1;
        }
        //  Debug.printPC("printing pc at the end of the path");
        return x;
    }


    public static int doubleBranchCov(int x, int y) {

        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y;
            else x = x + 1;
        }
        if (x == y)
            return 1;
        else return 0;
    }

    public static int unoptimalDFS(int x, int y) {
        if (x == y)
            return 1;

        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y + 3;
            else if (y > 1)
                x = x + 1;
            else
                x = x + 2;
        }
        return x;
    }


    public static int doubleLoopUnoptimalDFS(int x, int y) {
        int j = 0;
        if (x == y)
            return 1;

        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y + 3;
            else if (y > 1) {
                while (j < 4) {
                    x = x + 1;
                    j++;
                }
            } else
                x = x + 2;
        }
        return x;
    }


    public static int doubleLoop(int x, int y) {

        int j = 0;
        for (int i = 0; i < 2; i++) {
            while (j < 4) {
                x = x + 1;
                j++;
            }
        }
        return x;
    }


    public static int complexBranchCov(int x, int y) {

        for (int i = 0; i < 2; i++) {
            if (x == 1)
                if (x == y)
                    x = 0;
                else x = x + 1;
        }
        return x;
    }

    public static int branchMethodCov(int x, int y) {

        A a = new A();
        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y;
            else x = x + a.branchInA(true);
        }
        //  Debug.printPC("printing pc at the end of the path");
        return x;
    }

    public static int singleRec(int x, int y) {

        A a = new A();
        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y;
            else x = x + a.fib(x);
        }
        //  Debug.printPC("printing pc at the end of the path");
        return x;
    }

    public static int doubleRec(int x, int y) {

        A a = new A();
        for (int i = 0; i < 2; i++) {
            if (x == 1)
                x = y;
            else x = x + a.fibA(3);
        }
//          Debug.printPC("printing pc at the end of the path");
        return x;
    }

    public int testingSymbolicFields(int x, int y) {
        B b = new B();
        if (b.b == 3)
            y = y + 1;
        else
            y = y + 2;

        return y;
    }

}
