import weilantianhai.agent.ir.IRCommand;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.resolver.IRParser;

public class IRParserTest {
    public static void main(String[] args) throws Exception {
        String json = """
    {
      "version": 1,
      "operations": [
        {"type":"MOVE_CAMERA_POS","id":1,"no_result":false,
         "data":{"axis":"x","distance":-5}},
        {"type":"ROTATE_CAMERA_VIEW","id":2,
         "data":{"axis":"xw","angle":45}}
      ]
    }
    """;

        IRParser parser = new IRParser();
        IRBundle bundle = parser.parseAsBundle(json);
        System.out.println("version=" + bundle.getVersion());
        for (IRCommand cmd : bundle.getOperations()) {
            System.out.println(cmd.getType() + " id=" + cmd.getId()
                    + " noResult=" + cmd.isNoResult()
                    + " impl=" + cmd.getImpl().name());
        }
    }
}
