package bo.biogasgama

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.OutputStream

/**
 * Almacén local en formato OGC GeoPackage 1.3: una capa de puntos EPSG:4326
 * que QGIS/ArcGIS abren directamente.
 */
class GeoPackage(val archivo: File) {

    companion object {
        const val TABLA = "puntos_biogas"
        private const val ID_APLICACION = 0x47504B47 // "GPKG"
        private const val VERSION_GPKG = 10300
        private const val DESCRIPCION = "Levantamiento Biogas"
        private const val CAMPOS = "id, geom, fecha_hora, latitud, longitud, altitud_msnm, precision_m, temp_c, viento_vel, " +
            "viento_dir, ch4_lel, ch4_vol_pct, ch4_ppm, h2s_ppm, co_ppm, o2_vol_pct, potencial_biogas, observaciones"
        private const val COLUMNAS = """
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            geom POINT,
            fecha_hora TEXT,
            latitud DOUBLE,
            longitud DOUBLE,
            este_m DOUBLE,
            norte_m DOUBLE,
            altitud_msnm DOUBLE,
            precision_m DOUBLE,
            temp_c DOUBLE,
            viento_vel DOUBLE,
            viento_dir TEXT,
            ch4_lel DOUBLE,
            ch4_vol_pct DOUBLE,
            ch4_ppm DOUBLE,
            h2s_ppm DOUBLE,
            co_ppm DOUBLE,
            o2_vol_pct DOUBLE,
            potencial_biogas TEXT,
            observaciones TEXT"""
        private const val AHORA = "strftime('%Y-%m-%dT%H:%M:%fZ','now')"
        private const val WKT_UTM20S = "PROJCS[\"WGS 84 / UTM zone 20S\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563," +
            "AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
            "UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]," +
            "PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-63]," +
            "PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000]," +
            "UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AXIS[\"Easting\",EAST],AXIS[\"Northing\",NORTH],AUTHORITY[\"EPSG\",\"32720\"]]"
        private const val WKT_WGS84 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563," +
            "AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
            "UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]"
    }

    private fun <T> usar(bloque: (SQLiteDatabase) -> T): T = synchronized(this) {
        // NO_LOCALIZED_COLLATORS evita la tabla android_metadata, ajena al estándar.
        SQLiteDatabase.openDatabase(
            archivo.path, null,
            SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
        ).use { db ->
            db.disableWriteAheadLogging() // un único archivo .gpkg, sin -wal al exportar
            crearEsquema(db)
            quitarColumnaDesarrollador(db)
            pasarAUtm20S(db)
            bloque(db)
        }
    }

    private fun crearEsquema(db: SQLiteDatabase) {
        if (db.version == VERSION_GPKG) return
        db.beginTransaction()
        try {
            db.execSQL("PRAGMA application_id = $ID_APLICACION")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS gpkg_spatial_ref_sys (
                    srs_name TEXT NOT NULL, srs_id INTEGER NOT NULL PRIMARY KEY,
                    organization TEXT NOT NULL, organization_coordsys_id INTEGER NOT NULL,
                    definition TEXT NOT NULL, description TEXT)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS gpkg_contents (
                    table_name TEXT NOT NULL PRIMARY KEY, data_type TEXT NOT NULL,
                    identifier TEXT UNIQUE, description TEXT DEFAULT '',
                    last_change DATETIME NOT NULL DEFAULT ($AHORA),
                    min_x DOUBLE, min_y DOUBLE, max_x DOUBLE, max_y DOUBLE, srs_id INTEGER,
                    CONSTRAINT fk_gc_r_srs_id FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS gpkg_geometry_columns (
                    table_name TEXT NOT NULL, column_name TEXT NOT NULL, geometry_type_name TEXT NOT NULL,
                    srs_id INTEGER NOT NULL, z TINYINT NOT NULL, m TINYINT NOT NULL,
                    CONSTRAINT pk_geom_cols PRIMARY KEY (table_name, column_name),
                    CONSTRAINT uk_gc_table_name UNIQUE (table_name),
                    CONSTRAINT fk_gc_tn FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name),
                    CONSTRAINT fk_gc_srs FOREIGN KEY (srs_id) REFERENCES gpkg_spatial_ref_sys(srs_id))"""
            )
            val srs = "INSERT OR IGNORE INTO gpkg_spatial_ref_sys VALUES (?,?,?,?,?,?)"
            db.execSQL(srs, arrayOf("Undefined cartesian SRS", -1, "NONE", -1, "undefined", "undefined cartesian coordinate reference system"))
            db.execSQL(srs, arrayOf("Undefined geographic SRS", 0, "NONE", 0, "undefined", "undefined geographic coordinate reference system"))
            db.execSQL(srs, arrayOf("WGS 84 geodetic", GeometriaGpkg.SRS_WGS84, "EPSG", 4326, WKT_WGS84, "longitude/latitude coordinates in decimal degrees on the WGS 84 spheroid"))
            insertarSrsUtm(db)

            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLA ($COLUMNAS)")
            db.execSQL(
                "INSERT OR IGNORE INTO gpkg_contents (table_name, data_type, identifier, description, srs_id) VALUES (?,?,?,?,?)",
                arrayOf(TABLA, "features", TABLA, DESCRIPCION, Utm.SRS_UTM20S),
            )
            db.execSQL(
                "INSERT OR IGNORE INTO gpkg_geometry_columns VALUES (?,?,?,?,?,?)",
                arrayOf(TABLA, "geom", "POINT", Utm.SRS_UTM20S, 0, 0),
            )
            db.version = VERSION_GPKG
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Bases creadas antes de v1.7 tenían la columna "desarrollador": se reconstruye la tabla sin ella. */
    private fun quitarColumnaDesarrollador(db: SQLiteDatabase) {
        val tiene = db.rawQuery("PRAGMA table_info($TABLA)", null).use { c ->
            generateSequence { if (c.moveToNext()) c.getString(1) else null }.any { it == "desarrollador" }
        }
        if (!tiene) return
        db.beginTransaction()
        try {
            db.execSQL("ALTER TABLE $TABLA RENAME TO ${TABLA}_anterior")
            db.execSQL("CREATE TABLE $TABLA ($COLUMNAS)")
            db.execSQL("INSERT INTO $TABLA ($CAMPOS) SELECT $CAMPOS FROM ${TABLA}_anterior")
            db.execSQL("DROP TABLE ${TABLA}_anterior")
            db.execSQL("UPDATE gpkg_contents SET description = ? WHERE table_name = ?", arrayOf(DESCRIPCION, TABLA))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertarSrsUtm(db: SQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO gpkg_spatial_ref_sys VALUES (?,?,?,?,?,?)",
            arrayOf("WGS 84 / UTM zone 20S", Utm.SRS_UTM20S, "EPSG", 32720, WKT_UTM20S, "WGS 84 / UTM zona 20 Sur"),
        )
    }

    /** Bases creadas antes de v1.8 tenían la capa en EPSG:4326: se reproyecta a UTM 20S y se llenan este_m/norte_m. */
    private fun pasarAUtm20S(db: SQLiteDatabase) {
        val srsActual = db.rawQuery("SELECT srs_id FROM gpkg_geometry_columns WHERE table_name = ?", arrayOf(TABLA)).use { c ->
            if (c.moveToFirst()) c.getInt(0) else Utm.SRS_UTM20S
        }
        if (srsActual == Utm.SRS_UTM20S) return
        val columnas = db.rawQuery("PRAGMA table_info($TABLA)", null).use { c ->
            generateSequence { if (c.moveToNext()) c.getString(1) else null }.toSet()
        }
        db.beginTransaction()
        try {
            insertarSrsUtm(db)
            if ("este_m" !in columnas) db.execSQL("ALTER TABLE $TABLA ADD COLUMN este_m DOUBLE")
            if ("norte_m" !in columnas) db.execSQL("ALTER TABLE $TABLA ADD COLUMN norte_m DOUBLE")
            db.rawQuery("SELECT id, latitud, longitud FROM $TABLA", null).use { c ->
                while (c.moveToNext()) {
                    val (este, norte) = Utm.aUtm20S(c.getDouble(1), c.getDouble(2))
                    db.update(TABLA, ContentValues().apply {
                        put("geom", GeometriaGpkg.punto(este, norte, Utm.SRS_UTM20S))
                        put("este_m", este)
                        put("norte_m", norte)
                    }, "id = ?", arrayOf(c.getLong(0).toString()))
                }
            }
            db.execSQL("UPDATE gpkg_geometry_columns SET srs_id = ? WHERE table_name = ?", arrayOf(Utm.SRS_UTM20S, TABLA))
            db.execSQL("UPDATE gpkg_contents SET srs_id = ? WHERE table_name = ?", arrayOf(Utm.SRS_UTM20S, TABLA))
            actualizarExtension(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertar(r: Registro): Long = usar { db ->
        val (este, norte) = Utm.aUtm20S(r.latitud, r.longitud)
        val v = ContentValues().apply {
            put("geom", GeometriaGpkg.punto(este, norte, Utm.SRS_UTM20S))
            put("este_m", este)
            put("norte_m", norte)
            put("fecha_hora", r.fechaHora)
            put("latitud", r.latitud)
            put("longitud", r.longitud)
            put("altitud_msnm", r.altitudMsnm)
            put("precision_m", r.precisionM)
            put("temp_c", r.tempC)
            put("viento_vel", r.vientoVelMs)
            put("viento_dir", r.vientoDir)
            put("ch4_lel", r.ch4Lel)
            put("ch4_vol_pct", r.ch4VolPct)
            put("ch4_ppm", r.ch4Ppm)
            put("h2s_ppm", r.h2sPpm)
            put("co_ppm", r.coPpm)
            put("o2_vol_pct", r.o2VolPct)
            put("potencial_biogas", if (r.potencialBiogas) "SÍ" else "NO")
            put("observaciones", r.observaciones)
        }
        val id = db.insertOrThrow(TABLA, null, v)
        actualizarExtension(db)
        id
    }

    fun borrar(id: Long) = usar { db ->
        db.delete(TABLA, "id = ?", arrayOf(id.toString()))
        actualizarExtension(db)
    }

    fun listar(): List<Registro> = usar { db ->
        db.rawQuery(
            """SELECT id, fecha_hora, latitud, longitud, altitud_msnm, precision_m, temp_c, viento_vel, viento_dir,
                ch4_lel, h2s_ppm, co_ppm, o2_vol_pct, potencial_biogas, observaciones FROM $TABLA ORDER BY id DESC""",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Registro(
                        id = c.getLong(0),
                        fechaHora = c.getString(1) ?: "",
                        latitud = c.getDouble(2),
                        longitud = c.getDouble(3),
                        altitudMsnm = c.dobleONulo(4),
                        precisionM = c.dobleONulo(5),
                        tempC = c.dobleONulo(6),
                        vientoVelMs = c.dobleONulo(7),
                        vientoDir = c.getString(8),
                        ch4Lel = c.getDouble(9),
                        h2sPpm = c.getDouble(10),
                        coPpm = c.getDouble(11),
                        o2VolPct = c.getDouble(12),
                        potencialBiogas = c.getString(13) == "SÍ",
                        observaciones = c.getString(14) ?: "",
                    )
                )
            }
        }
    }

    /** Copia del .gpkg cerrado, lista para guardar o compartir. */
    fun copiarA(destino: OutputStream) {
        usar { } // garantiza que el archivo exista con su esquema
        synchronized(this) { archivo.inputStream().use { it.copyTo(destino) } }
    }

    private fun actualizarExtension(db: SQLiteDatabase) {
        db.execSQL(
            """UPDATE gpkg_contents SET
                min_x = (SELECT MIN(este_m) FROM $TABLA), max_x = (SELECT MAX(este_m) FROM $TABLA),
                min_y = (SELECT MIN(norte_m) FROM $TABLA), max_y = (SELECT MAX(norte_m) FROM $TABLA),
                last_change = $AHORA
                WHERE table_name = ?""",
            arrayOf(TABLA),
        )
    }

    private fun Cursor.dobleONulo(i: Int): Double? = if (isNull(i)) null else getDouble(i)
}
