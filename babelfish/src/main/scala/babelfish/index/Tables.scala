/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
 * Copyright (c) 2014 Plausible Labs Cooperative, Inc.
 *
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

import java.net.{InetAddress, Inet4Address, URISyntaxException, URI}
import java.nio.file.{Paths, Path}
import java.sql.Timestamp
import java.time.Instant

import com.google.common.net.InetAddresses

import scala.slick.driver.JdbcProfile

import scalaz.{\/-, \/}

/**
 * Provides access to the backing Index database profile.
 */
trait Database[Profile <: JdbcProfile] {
  /** The Slick database profile. */
  val profile: Profile

  /** The Slick database connection. */
  protected val db: Profile#Backend#Database

  import profile.simple._

  /**
   * Execute the given function within a new database session.
   *
   * @param f The function to be executed.
   */
  def withSession[T](f: Session => T): IndexError \/ T = {
    // TODO: Catch exceptions and return as a proper result?
    \/-(db.withSession(f))
  }

  /**
   * Execute the given function within a new database session, in a transaction.
   *
   * @param f The transaction function to be executed.
   */
  def withTransaction[T](f: Session => T): IndexError \/ T = {
    // TODO: Transaction deadlock detection and retry?
    \/-(db.withTransaction(f))
  }
}

/**
 * Code index table definitions.
 */
trait Tables[Profile <: JdbcProfile] { self:Database[Profile] =>
  import profile.simple._

  /**
   * URI type; provides a mapping from URI instances to a database String
   * representation.
   */
  implicit val uriColumnType = MappedColumnType.base[URI, String](
    { uri => uri.toASCIIString },
    { str =>
      try {
        new URI(str)
      } catch {
        case e:URISyntaxException => throw new SlickException(s"Invalid URI value: $str", e)
      }
    }
  )

  /**
   * IP address type; provides a mapping from Java InetAddress values to
   * a String representation.
   */
  implicit val ipv4ColumnType = MappedColumnType.base[InetAddress, String](
    { addr => addr.getHostAddress },
    { str =>
      try {
        InetAddresses.forString(str)
      } catch {
        case e:IllegalArgumentException => throw new SlickException(s"Invalid IP address value: $str", e)
      }
    }
  )

  /**
   * `Instant` type; provides a mapping from SQL Timestamp values to
   * Java 8's Instant type.
   */
  implicit val instantColumnType = MappedColumnType.base[Instant, Timestamp](Timestamp.from, _.toInstant)

  /**
   * `Path` type; provides a mapping from String values to Java 8's Path type.
   */
  implicit val pathColumnType = MappedColumnType.base[Path, String](_.toString, Paths.get(_))

  class OperatingSystems (tag: Tag) extends Table[(Int, String)](tag, "os") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name", O.NotNull)
    def * = (id, name)
  }
  val operatingSystems = TableQuery[OperatingSystems]

  class MajorReleases (tag: Tag) extends Table[(Int, Int, String, String)](tag, "major_release") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def osId = column[Int]("os_id")
    def name = column[String]("name")
    def majorVersion = column[String]("major_version")

    def * = (id, osId, name, majorVersion)

    def nameUniqueIdx = index("major_release_name_unique_idx", (osId, name), unique = true)
    def majorVersionUniqueIdx = index("major_release_version_unique_idx", (osId, majorVersion), unique = true)
    
    def operatingSystem = foreignKey("major_release_os_fkey", osId, operatingSystems)(_.id)
  }
  def majorReleases = TableQuery[MajorReleases]

  class MinorReleases (tag: Tag) extends Table[(Int, Int, String)](tag, "minor_release") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def major_release_id = column[Int]("major_release_id")
    def minorVersion = column[String]("version")

    def * = (id, major_release_id, minorVersion)

    def uniqueIdx = index("minor_release_major_release_id_version_idx", (major_release_id, minorVersion), unique = true)
    def majorRelease = foreignKey("minor_release_major_release_fkey", major_release_id, majorReleases)(_.id)
  }
  def minorReleases = TableQuery[MinorReleases]

  class Users (tag: Tag) extends Table[(Int, String, Int, Boolean)](tag, "user") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def datasetId = column[Int]("dataset_id")
    def isTestAccount = column[Boolean]("test_account")

    def * = (id, name, datasetId, isTestAccount)

    def uniqueIdx = index("user_name_unique_idx", (name, datasetId), unique = true)
    def dataset = foreignKey("user_dataset_fkey", datasetId, datasets)(_.id)
  }
  val users = TableQuery[Users]
  
  class Datasets (tag: Tag) extends Table[(Int, String, Int, String)](tag, "dataset") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name", O.NotNull)
    def version = column[Int]("version", O.NotNull)
    def description = column[String]("description", O.NotNull)
    def * = (id, name, version, description)
  }
  val datasets = TableQuery[Datasets]

  class Applications (tag: Tag) extends Table[(Int, Path, String, Option[String], Option[URI], Option[URI])](tag, "app") {
    def id = column[Int]("id", O.PrimaryKey)
    def path = column[Path]("path", O.NotNull)
    def name = column[String]("name", O.NotNull)
    def bundleId = column[Option[String]]("bundle_id", O.Nullable)
    def productURL = column[Option[URI]]("product_url", O.Nullable)
    def documentationURL = column[Option[URI]]("documentation_url", O.Nullable)
    def * = (id, path, name, bundleId, productURL, documentationURL)

    def pathUniqueIdx = index("app_path_idx", path, unique = true)
    def bundleUniqueIdx = index("app_bundle_id_idx", bundleId, unique = true)
  }
  val apps = TableQuery[Applications]

  class Connections (tag: Tag) extends Table[(Int, Int, Instant, Option[Int], Option[Int], InetAddress, Int, InetAddress, Int, Option[String])](tag, "connection") {
    def id = column[Int]("id", O.PrimaryKey)
    def dataset_id = column[Int]("dataset_id", O.NotNull)
    def timestamp = column[Instant]("timestamp", O.NotNull)
    def app_id = column[Option[Int]]("app_id", O.Nullable)
    def user_id = column[Option[Int]]("user_id", O.Nullable)
    def src_addr = column[InetAddress]("src_addr", O.NotNull)
    def src_port = column[Int]("src_port", O.NotNull)
    def dest_addr = column[InetAddress]("dest_addr", O.NotNull)
    def dest_port = column[Int]("dest_port", O.NotNull)
    def dest_host = column[Option[String]]("dest_host", O.Nullable)

    def * = (id, dataset_id, timestamp, app_id, user_id, src_addr, src_port, dest_addr, dest_port, dest_host)

    def dataset = foreignKey("connection_dataset_fkey", dataset_id, datasets)(_.id)
    def app = foreignKey("connection_app_fkey", dataset_id, datasets)(_.id)
    def user = foreignKey("connection_user_fkey", user_id, users)(_.id)

    // TODO -- Need constraint for (app_id.dataset_id == user_id.dataset_id == dataset_id)
  }
  val connections = TableQuery[Connections]

  /** Schema DDL for all tables */
  private[index] val ddl: profile.DDL = operatingSystems.ddl ++ majorReleases.ddl ++ minorReleases.ddl ++ datasets.ddl ++ apps.ddl
}