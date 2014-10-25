/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
 * Copyright (c) 2014 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package babelfish.index

import scala.slick.jdbc.meta.MTable
import scala.slick.driver.JdbcProfile
import scalaz.{-\/, \/-, \/}

private object Migrations {
  /**
   * A version record.
   *
   * @param version The version number.
   * @param compatible The earliest migration version with which this schema is compatible.
   */
  case class Version (version:Int, compatible:Int)
}

/**
 * Migration tables.
 */
private trait Migrations[Profile <: JdbcProfile] { self: Database[Profile] with Tables[Profile] =>
  import Migrations._
  import profile.simple._

  /* The version tracking table */
  private[index] class Versions (tag: Tag) extends Table[Version](tag, "migration_version") {
    def version = column[Int]("version", O.PrimaryKey)
    def compatVersion = column[Int]("compat_version")

    def * = (version, compatVersion) <> (Version.tupled, Version.unapply)

    def uniqueIdx = index("version_idx", version, unique = true)
  }
  private[index] val versions = TableQuery[Versions]

  /**
   * Migrate to the latest database version, and return the new version.
   *
   * @param current The current database version.
   * @return The new database version, or an error.
   */
  private def migrate (current: Version) (implicit session: Session): IndexError \/ Version = {
    current match {
      /* Initial migration */
      case Version(0, _) =>
        (versions.ddl ++ ddl).create
        \/-(Version(CURRENT_VERSION, CURRENT_COMPAT))

      /* Handle newer versions that are compatible with this release */
      case Version(_, compat) if compat <= CURRENT_VERSION => \/-(current)

      /* Handle newer versions that are incompatible with this release */
      case other => -\/(InvalidDatabase(s"Index version $other is unsupported by this release of babelfish"))
    }
  }

  /**
   * Apply any migrations required in a transaction, returning either the new database version, or an error.
   */
  def applyMigrations (): IndexError \/ Version = {
    db.withTransaction{ implicit session =>
      val INITIAL_VERSION = Version(0, 0)
      val version = if (MTable.getTables("migration_version").list.isEmpty) {
        /* Migrations table is doesn't exist */
        INITIAL_VERSION
      } else {
        versions.sortBy(_.version.desc).firstOption.getOrElse {
          /* Mirations table is empty */
          INITIAL_VERSION
        }
      }

      val result = migrate(version)
      result.foreach { newVersion =>
        if (newVersion.version != version.version)
          versions.insert(newVersion)
      }
      result
    }
  }

  /** The current compatibility number of the DDL. */
  private val CURRENT_COMPAT = 1

  /** The current version number of the DDL. */
  private val CURRENT_VERSION = 1
}