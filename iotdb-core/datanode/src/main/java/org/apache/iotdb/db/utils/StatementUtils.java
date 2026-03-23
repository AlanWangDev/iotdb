package org.apache.iotdb.db.utils;

import org.apache.iotdb.db.queryengine.plan.relational.sql.ast.Copy;
import org.apache.iotdb.db.queryengine.plan.relational.sql.ast.Statement;

/** Utility class for handling Statement objects. */
public class StatementUtils {

  /**
   * Retrieves the underlying statement. If the given statement is a Copy statement, returns the
   * query inside the Copy; otherwise, returns the statement itself.
   *
   * @param statement the input Statement object
   * @return the underlying Statement or the query if it's a Copy
   */
  public static Statement getUnderlyingStatement(Statement statement) {
    if (statement instanceof Copy) {
      return ((Copy) statement).getQuery();
    }
    return statement;
  }
}
