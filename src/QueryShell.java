import lib.UserQueryParser;

public class QueryShell {
    private UserQueryParser parser;

    public QueryShell() {
        parser = new UserQueryParser();
    }

    public String run(String sql) {
        return parser.execute(sql);
    }
}