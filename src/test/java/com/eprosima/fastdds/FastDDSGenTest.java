package test.com.eprosima.fastdds;

import org.junit.jupiter.api.Test;

import com.eprosima.integration.Command;

import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.nio.file.Paths;
import java.nio.file.Files;

public class FastDDSGenTest
{
    //private static final String INPUT_PATH = "thirdparty/idl-parser/test/idls";
    private static final String INPUT_PATH = "thirdparty/dds-types-test/IDL";
    private static final String OUTPUT_PATH = "build/test/integration/idls";

    private static boolean isUnix()
    {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    @Test
    public void Context_getRelativeDir_Test()
    {
        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "Prueba.idl", new ArrayList<String>());

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "dir/Prueba.idl", new ArrayList<String>());

            assertEquals("dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "../../dir/Prueba.idl", new ArrayList<String>());

            assertEquals("../../dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    System.getProperty("user.dir") + "Prueba.idl", new ArrayList<String>());

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    System.getProperty("user.dir") + "dir/Prueba.idl", new ArrayList<String>());

            assertEquals("dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    System.getProperty("user.dir") + "../../dir/Prueba.idl", new ArrayList<String>());

            assertEquals("../../dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "/home/testing/Prueba.idl", new ArrayList<String>());

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "/home/testing/Prueba.idl", new ArrayList<String>());

            assertEquals("", ctx.getRelativeDir("/home/testing/"));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    "/home/testing/Prueba.idl", new ArrayList<String>());

            assertEquals("testing/", ctx.getRelativeDir("/home/"));
        }
    }

    @Test
    public void runTests()
    {
        if (!isUnix())
        {
            System.out.println("WARNING: The tests are only available with an unix system");
            return;
        }

        //Configure idl tests
        TestManager tests = new TestManager(TestLevel.RUN, "share/fastddsgen/java/fastddsgen", INPUT_PATH,
                        OUTPUT_PATH, "CMake");
        tests.addCMakeArguments("-DCMAKE_BUILD_TYPE=Debug");
        tests.removeTests("basic_inner_types");



        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
