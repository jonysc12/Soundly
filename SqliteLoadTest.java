import java.sql.*;
public class SqliteLoadTest {
  public static void main(String[] args) throws Exception {
    Class.forName("org.sqlite.JDBC");
    Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
    c.createStatement().execute("create table t(id integer)");
    c.close();
    System.out.println("OK");
  }
}
