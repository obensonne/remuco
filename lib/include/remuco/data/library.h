#ifndef LIBRARY_H_
#define LIBRARY_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

typedef struct _RemLibrary	RemLibrary;

typedef enum {
	REM_LIBRARY_FLAG_EDITABLE =	1 << 0
} rem_library_flags;

RemLibrary*
rem_library_new(void);

void
rem_library_destroy(RemLibrary *lib);

void
rem_library_clear(RemLibrary *lib);

void
rem_library_append(RemLibrary *lib, gchar *plid, gchar *name, gint flags);

void
rem_library_append_const(RemLibrary *lib,
						 const gchar *plid,
						 const gchar *name,
						 gint flags);

#define rem_library_dump(_pls)	LOG_WARN("library dump not implemented\n")

G_END_DECLS

#endif /*LIBRARY_H_*/
