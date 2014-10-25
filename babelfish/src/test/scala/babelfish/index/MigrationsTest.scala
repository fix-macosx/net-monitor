/*
 * Copyright (c) 2014 Landon Fuller <landon@landonf.org>
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

import java.util.UUID

import babelfish.index.Migrations.Version
import org.specs2.mutable.Specification

import scala.slick.driver.H2Driver

class MigrationsTest extends Specification {
  /* Hack up in-memory migration handler */
  private class MemoryMigrations extends Database[H2Driver] with Tables[H2Driver] with Migrations[H2Driver] {
    override lazy val profile = H2Driver

    override val db = {
      import profile.simple._
      /* H2 mem paths are process-wide, so we have to generate something hopefully unique. */
      val url = "jdbc:h2:mem:" + "code-index-" + UUID.randomUUID().toString + ";DB_CLOSE_DELAY=-1"
      Database.forURL(url, driver = "org.h2.Driver")
    }
  }

  "Migration implementation" should {
    "initialize the default DDL" in {
      val migrations = new MemoryMigrations()
      migrations.applyMigrations().toOption must beSome

      migrations.db.withSession { implicit session =>
        import H2Driver.profile.simple._
        (for (version <- migrations.versions) yield version).first
      } must beEqualTo(Version(1, 1))
    }
  }
}
