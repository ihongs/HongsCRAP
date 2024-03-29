package io.github.ihongs.db.diff;

import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表结构同步
 *
 * <b>注意: 参考MySQL编写, 可能不适用于其他数据库(使用了SHOW语句)</b>
 *
 * @author Hongs
 */
public class TableDiff
{

  private final Table table;
  private final TableForm tableDesc;

  /**
   * 通过表对象构造
   * @param table
   * @throws io.github.ihongs.CruxException
   */
  public TableDiff(Table table)
  throws CruxException
  {
    this.table = table;
    this.tableDesc = TableForm.getInstance(table);
  }

  /**
   * 同步从表结构
   * @param slaver
   * @param delExtraFields 删除多余的字段
   * @throws io.github.ihongs.CruxException
   */
  public void syncSlaver(Table slaver, boolean delExtraFields)
  throws CruxException
  {
    List<String> sqls = this.deffSlaver(slaver, delExtraFields);
    DB sdb = slaver.db;
    sdb.begin();
    try
    {
      for (String sql : sqls)
      {
        sdb.execute(sql);
      }
      sdb.commit();
    }
    catch (CruxException ex )
    {
      sdb.cancel();
      throw ex;
    }
  }

  public List<String> deffSlaver(Table slaver, boolean delExtraFields)
  throws CruxException
  {
    List sqls = new ArrayList();

    // 没有表则创建表
    String sql = "SHOW TABLES LIKE '"+slaver.tableName+"'";
    if (slaver.db.fetchAll(sql).isEmpty())
    {
        sql = "SHOW CREATE TABLE `"+table.tableName+"`";
        sql = ((Map) table.db.fetchAll(sql).get( 0 ))
                    .get("Create Table").toString( );
        sql = sql.replaceFirst("^CREATE TABLE `.*?`",
              "CREATE TABLE `"+slaver.tableName+"`");

        sqls.add(sql);
        return   sqls;
    }

    TableForm slaverDesc = TableForm.getInstance(slaver);
    Iterator  it;
    Map.Entry et;

    /**
     * 第一步:
     * 根据子表对比主表结构
     * 找出缺失或改变了的键并删除
     * 找出缺失的字段并删除
     */

      // 主键
      if (tableDesc.priCols.isEmpty())
      {
        sql = tableDesc.alterPriKeySql(slaver.tableName, TableForm.DROP);
        sqls.add(sql);
      }

      // 唯一键
      it = slaverDesc.uniKeys.entrySet().iterator();
      while (it.hasNext())
      {
        et = (Map.Entry)it.next();
        String key = (String)et.getKey();

        if (!tableDesc.uniKeys.containsKey(key))
        {
          sql = tableDesc.alterUniKeySql(slaver.tableName, TableForm.DROP, key);
          sqls.add(sql);
        }
      }

      // 索引键
      it = slaverDesc.idxKeys.entrySet().iterator();
      while (it.hasNext())
      {
        et = (Map.Entry)it.next();
        String key = (String)et.getKey();

        if (!tableDesc.idxKeys.containsKey(key))
        {
          sql = tableDesc.alterIdxKeySql(slaver.tableName, TableForm.DROP, key);
          sqls.add(sql);
        }
      }

    if (delExtraFields)
    {
      // 字段
      it = slaverDesc.columns.entrySet().iterator();
      while (it.hasNext())
      {
        et = (Map.Entry)it.next();
        String col = (String)et.getKey();

        if (!tableDesc.columns.containsKey(col))
        {
          sql = tableDesc.alterColumnSql(slaver.tableName, TableForm.DROP, col);
          sqls.add(sql);
        }
      }
    }

    /**
     * 第二步:
     * 根据主表对比子表结构
     * 找出新增的字段并添加
     * 找出不同的字段并更新
     */

    it = tableDesc.columns.entrySet().iterator();
    while (it.hasNext())
    {
      et = (Map.Entry)it.next();
      String col = (String)et.getKey();
      String dfn = (String)et.getValue();

      if (! slaverDesc.columns.containsKey(col))
      {
        sql = tableDesc.alterColumnSql(slaver.tableName, TableForm.ADD, col);
        sqls.add(sql);
      }
      else if (! dfn.equals(slaverDesc.columns.get(col)))
      {
        sql = tableDesc.alterColumnSql(slaver.tableName, TableForm.MODIFY, col);
        sqls.add(sql);
      }
    }

    /**
     * 第三步:
     * 根据主表对比
     * 找出新增的键并添加
     */

    // 主键
    if (!tableDesc.priCols.isEmpty()
    &&  !tableDesc.priCols.equals(slaverDesc.priCols))
    {
      sql = tableDesc.alterPriKeySql(slaver.tableName, TableForm.ADD);
      sqls.add(sql);
    }

    // 唯一键
    it = tableDesc.uniKeys.entrySet().iterator();
    while (it.hasNext())
    {
      et = (Map.Entry)it.next();
      String key = (String)et.getKey();
      Set cols = (Set)et.getValue();

      if (!slaverDesc.uniKeys.containsKey(key)
      ||  !slaverDesc.uniKeys.get(key).equals(cols))
      {
        sql = tableDesc.alterUniKeySql(slaver.tableName, TableForm.ADD, key);
        sqls.add(sql);
      }
    }

    // 索引键
    it = tableDesc.idxKeys.entrySet().iterator();
    while (it.hasNext())
    {
      et = (Map.Entry)it.next();
      String key = (String)et.getKey();
      Set cols = (Set)et.getValue();

      if (!slaverDesc.idxKeys.containsKey(key)
      ||  !slaverDesc.idxKeys.get(key).equals(cols))
      {
        sql = tableDesc.alterIdxKeySql(slaver.tableName, TableForm.ADD, key);
        sqls.add(sql);
      }
    }

    return sqls;
  }

}
