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

import scala.slick.driver.{JdbcProfile, H2Driver}
import H2Driver.profile.simple._

import java.net.{URISyntaxException, URI}
import java.util.UUID

import scalaz.{-\/, \/}

import scalaz._, Scalaz._

import coop.plausible.nx.assertNonThrows

/**
 * Protocol index constructors
 */
object ConnectionIndex {
  /**
   * Open or create an index at the given H2 JDBC URL.
   *
   * @param uri An H2-compatible JDBC URL
   */
  private def withURI (uri: URI): IndexError \/ ConnectionIndex = {
    import scala.util.{Try, Success, Failure}
    Try(Database.forURL(uri.toASCIIString, driver = "org.h2.Driver")) match {
      case Success(db) => {
        val indexDB = new IndexDB[JdbcProfile](db, H2Driver)
        indexDB.applyMigrations().map(_ => new ConnectionIndex(indexDB))
      }
      case Failure(error) => -\/(new IndexError("Failed to open database", Some(error)))
    }
  }

  /**
   * Open and and return an empty in-memory index.
   */
  def withTemporaryDatabase (): IndexError \/ ConnectionIndex = {
    /* H2 mem paths are process-wide, so we have to generate something hopefully unique. */
    withURI(assertNonThrows[URISyntaxException](new URI("jdbc:h2:mem:code-index-" + UUID.randomUUID().toString + ";DB_CLOSE_DELAY=-1")))
  }

  /**
   * Open or create an index at the given path prefix. The actual database files will have a suffix appended.
   *
   * @param path An absolute path to the target file.
   */
  def withPath (path: String): IndexError \/ ConnectionIndex = try {
      val uri = new URI("jdbc:h2:" + path)
      withURI(new URI("jdbc:h2:" + path))
  } catch {
      case use:URISyntaxException => -\/(new IndexError(s"The provided database path '$path' could not be parsed as a URI", Some(use)))
  }

  /**
   * The index database.
   *
   * @param db Slick database.
   * @param profile Slick profile.
   * @tparam P Slick profile type
   */
  private class IndexDB[P <: JdbcProfile] (protected val db: P#Backend#Database, val profile:P) extends Database[P] with Tables[P] with Migrations[P]
}



/**
 * Connection index.
 *
 * @param db The backing index database.
 */
class ConnectionIndex private (db: ConnectionIndex.IndexDB[JdbcProfile]) {

}

