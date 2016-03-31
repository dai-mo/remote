import sbt._

object Global {
  // Versions
  lazy val scVersion = "2.11.7"  
  
  // Repositories
  val localMavenRepository = (
        "Local Maven Repository" at "file://" +
    		  Path.userHome.absolutePath +
    			"/.m2/repository"
    )
    
  val localIvyRepository = (
        "Local Ivy Repository" at "file://" +
    		  Path.userHome.absolutePath +
    			"/.ivy2/cache"
    )

}