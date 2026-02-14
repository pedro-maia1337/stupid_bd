// Generated from src/lib/grammar/SQLiteSimple.g4 by ANTLR 4.13.1

package lib.parser;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SQLiteSimpleParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SQLiteSimpleVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(SQLiteSimpleParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#sql_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_stmt(SQLiteSimpleParser.Sql_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#select_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_stmt(SQLiteSimpleParser.Select_stmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SelectAll}
	 * labeled alternative in {@link SQLiteSimpleParser#result_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectAll(SQLiteSimpleParser.SelectAllContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SelectCount}
	 * labeled alternative in {@link SQLiteSimpleParser#result_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectCount(SQLiteSimpleParser.SelectCountContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SelectColumns}
	 * labeled alternative in {@link SQLiteSimpleParser#result_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectColumns(SQLiteSimpleParser.SelectColumnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(SQLiteSimpleParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_clause(SQLiteSimpleParser.Order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#group_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_clause(SQLiteSimpleParser.Group_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#insert_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt(SQLiteSimpleParser.Insert_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#update_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_stmt(SQLiteSimpleParser.Update_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(SQLiteSimpleParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#delete_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_stmt(SQLiteSimpleParser.Delete_stmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(SQLiteSimpleParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LikeExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLikeExpr(SQLiteSimpleParser.LikeExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqualsExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualsExpr(SQLiteSimpleParser.EqualsExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BetweenExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetweenExpr(SQLiteSimpleParser.BetweenExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpr(SQLiteSimpleParser.ParenExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrExpr}
	 * labeled alternative in {@link SQLiteSimpleParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(SQLiteSimpleParser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#literal_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_value(SQLiteSimpleParser.Literal_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#table_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_name(SQLiteSimpleParser.Table_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteSimpleParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(SQLiteSimpleParser.Column_nameContext ctx);
}