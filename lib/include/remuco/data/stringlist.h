#ifndef REMUCO_SL_H_
#define REMUCO_SL_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * @defgroup dx_RemSL String List
 * @ingroup dx_dt
 * 
 * A RemStringList is a data type to efficiently store strings in a list.
 */ 

/*@{*/

/**
 * The RemStringList struct is an opaque data structure to represent a string
 * list.
 */
typedef struct _RemStringList RemStringList;

/**
 * Creates a new RemStringList.
 * 
 * Use rem_sl_destroy() to free.
 */
RemStringList*
rem_sl_new();

/**
 * Frees all of the memory used by a RemStringList (this includes all contained
 * strings).
 */
void
rem_sl_destroy(RemStringList *sl);

/**
 * Appends a string to a RemStringList.
 * 
 * The RemStringList takes ownership of @a str and frees it when
 * rem_sl_destroy() gets called.
 * 
 * @param sl the RemStringList to append the string to
 * @param str the string to append
 */ 
void
rem_sl_append(RemStringList *sl, gchar *str);

/**
 * Appends a copy of a string to a RemStringList.
 * 
 * @param sl a RemStringList
 * @param str the string to append a copy of
 */ 
void
rem_sl_append_const(RemStringList *sl, const gchar *str);

/**
 * Removes all strings from a RemStringList.
 * 
 * @param sl a RemStringList
 */ 
void
rem_sl_clear(RemStringList *sl);

/**
 * Resets a RemStringList its iterator.
 * 
 * Allways call this before iterating the list with rem_sl_iterator_next().
 * 
 * @param sl a RemStringList
 */ 
void
rem_sl_iterator_reset(RemStringList *sl);

/**
 * Iterates to the next string in a RemStringList.
 * 
 * Allways call this before iterating the list with rem_sl_iterator_next().
 * 
 * @param sl a RemStringList
 * 
 * @return the next string or <code>NULL</code> if there is no more string
 */ 
const gchar*
rem_sl_iterator_next(RemStringList *sl);

/**
 * Returns the string of a RemStringList at the given index.
 * 
 * Do not use this function to iterate over the indivdual strings. Using
 * rem_sl_iterator_reset() and rem_sl_iterator_next() is more efficient.
 * 
 * @param sl a RemStringList
 * @param index the index of the string to return
 * 
 * @return the string at the given index or <code>NULL</code> if index is off
 *         the end of the RemStringList
 */ 
const gchar*
rem_sl_get(const RemStringList *sl, guint index);

/**
 * Returns the number of string in a RemStringList.
 * 
 * @param sl a RemStringList
 * 
 * @return the number of strings
 */
guint
rem_sl_length(const RemStringList *sl);

/**
 * Returns a hash value of a RemStringList.
 * 
 * @param sl a RemStringList
 * 
 * @return the hash value
 */
guint
rem_sl_hash(const RemStringList *sl);

/**
 * Compares 2 string lists and returns @p TRUE if they equal.
 * 
 * @param sl1 a RemStringList (may be <code>NULL</code>)
 * @param sl2 another RemStringList (may be <code>NULL</code>)
 * 
 * @return @p TRUE if @a sl1 and @a sl2 equal, @p FALSE otherwise
 */
gboolean
rem_sl_equal(const RemStringList *sl1, const RemStringList *sl2);

/**
 * Creates a copy of a RemStringList.
 * 
 * The copy is a @em real copy, i.e. the contained strings get copied too.
 * 
 * @param sl a RemStringList
 * 
 * @return a copy of @a sl
 */
RemStringList*
rem_sl_copy(const RemStringList *sl);

/**
 * Prints the content of a RemStringList to standard out.
 * 
 * @param sl a RemStringList
 */
void
rem_sl_dump(const RemStringList *sl);

/*@}*/

G_END_DECLS

#endif /*REMUCO_SL_H_*/
