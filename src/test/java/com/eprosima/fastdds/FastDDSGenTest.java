package test.com.eprosima.fastdds;

import org.junit.jupiter.api.Test;

import com.eprosima.idl.generator.manager.TemplateManager;

import com.eprosima.integration.Command;

import com.eprosima.integration.TestManager;
import com.eprosima.integration.TestManager.TestLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.nio.file.Paths;
import java.nio.file.Files;

public class FastDDSGenTest
{

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
        String absolute_idl_dir = isUnix() ?
            "/home/testing/Prueba.idl" : "C:/Users/testing/Prueba.idl";
        String absolute_dir = isUnix() ?
            "/home/testing/" : "C:/Users/testing/";
        String absolute_root_dir = isUnix() ?
            "/home/" : "C:/Users/";

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), "Prueba.idl", new ArrayList<String>(), false);

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), "dir/Prueba.idl", new ArrayList<String>(), false);

            assertEquals("dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), "../../dir/Prueba.idl", new ArrayList<String>(), false);

            assertEquals("../../dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), System.getProperty("user.dir") + "Prueba.idl", new ArrayList<String>(), false);

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), System.getProperty("user.dir") + "dir/Prueba.idl", new ArrayList<String>(), false);

            assertEquals("dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), System.getProperty("user.dir") + "../../dir/Prueba.idl", new ArrayList<String>(), false);

            assertEquals("../../dir/", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), absolute_idl_dir, new ArrayList<String>(), false);

            assertEquals("", ctx.getRelativeDir(null));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), absolute_idl_dir, new ArrayList<String>(), false);

            assertEquals("", ctx.getRelativeDir(absolute_dir));
        }

        {
            com.eprosima.idl.context.Context ctx = new com.eprosima.idl.context.Context(
                    new TemplateManager(), absolute_idl_dir, new ArrayList<String>(), false);

            assertEquals("testing/", ctx.getRelativeDir(absolute_root_dir));
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

        String list_tests_str = System.getProperty("list_tests");
        java.util.List<String> list_tests = null;

        if (null != list_tests_str)
        {
            list_tests = java.util.Arrays.asList(list_tests_str.split(",", -1));
        }

        String blacklist_tests_str = System.getProperty("blacklist_tests");
        java.util.List<String> blacklist_tests = null;

        if (null != blacklist_tests_str)
        {
            blacklist_tests = java.util.Arrays.asList(blacklist_tests_str.split(",", -1));
        }

        //Configure idl tests
        TestManager tests = new TestManager(
                TestLevel.RUN,
                "share/fastddsgen/java/fastddsgen",
                INPUT_PATH,
                OUTPUT_PATH,
                "CMake",
                list_tests,
                blacklist_tests);
        tests.addCMakeArguments("-DCMAKE_BUILD_TYPE=Debug");
        tests.removeTests("basic_inner_types");

        boolean testResult = tests.runTests();
        System.exit(testResult ? 0 : -1);
    }
}
