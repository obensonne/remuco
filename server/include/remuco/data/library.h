#ifndef REMUCO_LIBRARY_H_
#define REMUCO_LIBRARY_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

typedef struct _RemLibrary	RemLibrary;

typedef enum {
	REM_PLOBLIST_FLAG_STATIC	= 0,
	REM_PLOBLIST_FLAG_EDITABLE	= 1 << 0,
	REM_PLOBLIST_FLAG_DYNAMIC	= 1 << 1,
	REM_PLOBLIST_FLAG_STREAM	= 1 << 2,
	REM_PLOBLIST_FLAG_WEIRD		= 1 << 3,
} RemPloblistFlag;

RemLibrary*
rem_library_new(void);

void
rem_library_destroy(RemLibrary *lib);

void
rem_library_clear(RemLibrary *lib);

void
rem_library_append(RemLibrary *lib,
				   gchar *plid,
				   gchar *name,
				   RemPloblistFlag flags);

void
rem_library_append_const(RemLibrary *lib,
						 const gchar *plid,
						 const gchar *name,
						 RemPloblistFlag flags);

#define rem_library_dump(_pls)	LOG_WARN("library dump not implemented\n")

G_END_DECLS

#endif /*REMUCO_LIBRARY_H_*/
