/**
 * 
 */
package unknow.sync;

/**
 * the Query id
 * 
 * @author unknow
 */
public enum Query {
	/** first request */
	LOGIN,
	/** get bloc for a file */
	FILEBLOC,
	/** download a bloc data */
	GETBLOC,
	/** download a full file */
	GETFILE;
}