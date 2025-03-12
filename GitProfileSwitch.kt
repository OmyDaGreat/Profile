import java.io.File

/**
 * Data class representing a Git profile with a name and email.
 *
 * @property name The name associated with the Git profile.
 * @property email The email associated with the Git profile.
 */
data class GitProfile(
    val name: String,
    val email: String,
)

/**
 * Class responsible for managing Git profiles.
 */
class GitProfileSwitch {
    private val profilesFilePath = System.getProperty("user.home") + "/.git_profiles.yaml"
    private val profilesFile = File(profilesFilePath)

    /**
     * Switches to a selected Git profile.
     * If the profiles file does not exist, it generates a new one.
     */
    fun switchProfile() {
        if (!profilesFile.exists()) {
            println("Profiles file not found. Generating a new one.")
            generateProfilesFile()
            return
        }

        val profiles = parseProfiles(profilesFile)
        if (profiles.isEmpty()) {
            println("No profiles found in the profiles file.")
            return
        }

        println("Available profiles:")
        println("0. Add a new profile")
        profiles.keys.forEachIndexed { index, profileName ->
            println("${index + 1}. $profileName")
        }

        print("Select a profile by number: ")
        val selectedProfileIndex = readLine()?.toIntOrNull()?.minus(1)

        when (selectedProfileIndex) {
            -1 -> addNewProfile(profilesFile)
            null, !in profiles.keys.indices -> println("Invalid selection.")
            else -> {
                val selectedProfileName = profiles.keys.elementAt(selectedProfileIndex)
                val selectedProfile = profiles[selectedProfileName]

                if (selectedProfile == null) {
                    println("Profile not found: $selectedProfileName")
                    return
                }

                setGitConfig("user.name", selectedProfile.name)
                setGitConfig("user.email", selectedProfile.email)

                println("Switched to profile: $selectedProfileName")
            }
        }
    }

    /**
     * Parses the profiles from the given file.
     *
     * @param file The file containing the profiles.
     * @return A map of profile names to GitProfile objects.
     */
    private fun parseProfiles(file: File): Map<String, GitProfile> {
        val profiles = mutableMapOf<String, GitProfile>()
        var currentProfileName: String? = null
        var currentName: String? = null
        var currentEmail: String? = null

        file.forEachLine { line ->
            line.apply {
                when {
                    startsWith(" ") -> {
                        when {
                            line.trim().startsWith("name:") -> {
                                currentName = line.substringAfter("name:").trim()
                            }
                            line.trim().startsWith("email:") -> {
                                currentEmail = line.substringAfter("email:").trim()
                            }
                        }
                    }
                    isNotBlank() -> {
                        if (currentProfileName != null && currentName != null && currentEmail != null) {
                            profiles[currentProfileName!!] = GitProfile(currentName!!, currentEmail!!)
                        }
                        currentProfileName = line.trim().removeSuffix(":")
                        currentName = null
                        currentEmail = null
                    }
                }
            }
        }

        if (currentProfileName != null && currentName != null && currentEmail != null) {
            profiles[currentProfileName] = GitProfile(currentName!!, currentEmail!!)
        }

        return profiles
    }

    /**
     * Adds a new profile by prompting the user for input.
     *
     * @param file The file to which the new profile will be added.
     */
    private fun addNewProfile(file: File) {
        print("Enter profile name: ")
        val profileName = readLine()?.trim()
        print("Enter name: ")
        val name = readLine()?.trim()
        print("Enter email: ")
        val email = readLine()?.trim()

        if (profileName.isNullOrBlank() || name.isNullOrBlank() || email.isNullOrBlank()) {
            println("Invalid input. All fields are required.")
            return
        }

        addProfileIndependent(file, profileName, name, email)
    }

    /**
     * Adds a new profile with the given details.
     *
     * @param profileName The name of the profile.
     * @param name The name associated with the profile.
     * @param email The email associated with the profile.
     */
    internal fun addNewProfile(
        profileName: String,
        name: String,
        email: String,
    ) {
        if (profileName.isBlank() || name.isBlank() || email.isBlank()) {
            println("Invalid input. All fields are required.")
            return
        }

        addProfileIndependent(profilesFile, profileName, name, email)
    }

    /**
     * Adds a profile to the file independently.
     *
     * @param file The file to which the profile will be added.
     * @param profileName The name of the profile.
     * @param name The name associated with the profile.
     * @param email The email associated with the profile.
     */
    private fun addProfileIndependent(
        file: File,
        profileName: String,
        name: String,
        email: String,
    ) {
        val profiles = parseProfiles(file).toMutableMap()
        profiles[profileName] = GitProfile(name, email)

        file.writeText(
            profiles.entries.joinToString("\n\n") { (profileName, profile) ->
                "profile: $profileName\n  name: ${profile.name}\n  email: ${profile.email}"
            },
        )

        println("Profile '$profileName' added successfully.")
    }

    /**
     * Sets the Git configuration for the given key and value.
     *
     * @param key The Git configuration key.
     * @param value The value to set for the key.
     */
    private fun setGitConfig(
        key: String,
        value: String,
    ) {
        val process =
            ProcessBuilder("git", "config", "--global", key, value)
                .redirectErrorStream(true)
                .start()
        process.inputStream.bufferedReader().use { it.lines().forEach(::println) }
        process.waitFor()
    }

    /**
     * Generates a new profiles file with a default profile.
     */
    fun generateProfilesFile() {
        if (profilesFile.exists()) {
            println("Profiles file already exists at $profilesFilePath")
            return
        }

        profilesFile.writeText(
            """
            personal
              name: Your Name
              email: your.email@example.com
            """.trimIndent(),
        )

        println("Generated new profiles file at $profilesFilePath, make sure to edit it")
    }

    /**
     * Updates an existing profile.
     *
     * @param profileName The name of the profile to update.
     * @param newName The new name to set.
     * @param newEmail The new email to set.
     */
    fun updateProfile(
        profileName: String,
        newName: String,
        newEmail: String,
    ) {
        val profiles = parseProfiles(profilesFile).toMutableMap()
        if (profiles.containsKey(profileName)) {
            profiles[profileName] = GitProfile(newName, newEmail)
            saveProfilesToFile(profiles)
            println("Profile '$profileName' updated successfully.")
        } else {
            println("Profile '$profileName' not found.")
        }
    }

    /**
     * Deletes an existing profile.
     *
     * @param profileName The name of the profile to delete.
     */
    fun deleteProfile(profileName: String) {
        val profiles = parseProfiles(profilesFile).toMutableMap()
        if (profiles.remove(profileName) != null) {
            saveProfilesToFile(profiles)
            println("Profile '$profileName' deleted successfully.")
        } else {
            println("Profile '$profileName' not found.")
        }
    }

    /**
     * Saves the profiles to the file.
     *
     * @param profiles The map of profiles to save.
     */
    private fun saveProfilesToFile(profiles: Map<String, GitProfile>) {
        profilesFile.writeText(
            profiles.entries.joinToString("\n\n") { (profileName, profile) ->
                "$profileName:\n  name: ${profile.name}\n  email: ${profile.email}"
            },
        )
    }

    /**
     * Displays the contents of the config file.
     */
    fun viewConfigFile() {
        if (profilesFile.exists()) {
            println(profilesFile.readText())
        } else {
            println("Profiles file not found at $profilesFilePath")
        }
    }

    /**
     * Prints the help message with available commands.
     */
    fun printHelp() {
        println(
            """
            Available commands:
            add <profileName> <name> <email> - Add a new profile
            generate - Generate a new profiles file
            switch - Switch to a different profile
            update <profileName> <newName> <newEmail> - Update an existing profile
            delete <profileName> - Delete an existing profile
            view - View the contents of the config file
            help - Show this help message
            """.trimIndent(),
        )
    }
}

/**
 * Handles the add command to add a new Git profile.
 *
 * @param args The command-line arguments.
 * @param gitProfileSwitch The GitProfileSwitch instance.
 */
private fun handleAddCommand(
    args: Array<String>,
    gitProfileSwitch: GitProfileSwitch,
) {
    if (args.size == 4) {
        val profileName = args[1]
        val name = args[2]
        val email = args[3]
        gitProfileSwitch.addNewProfile(profileName, name, email)
    } else {
        println("Usage: profile add <profileName> <name> <email>")
    }
}

/**
 * Handles the update command to update an existing Git profile.
 *
 * @param args The command-line arguments.
 * @param gitProfileSwitch The GitProfileSwitch instance.
 */
private fun handleUpdateCommand(
    args: Array<String>,
    gitProfileSwitch: GitProfileSwitch,
) {
    if (args.size == 4) {
        val profileName = args[1]
        val newName = args[2]
        val newEmail = args[3]
        gitProfileSwitch.updateProfile(profileName, newName, newEmail)
    } else {
        println("Usage: profile update <profileName> <newName> <newEmail>")
    }
}

/**
 * Handles the delete command to delete an existing Git profile.
 *
 * @param args The command-line arguments.
 * @param gitProfileSwitch The GitProfileSwitch instance.
 */
private fun handleDeleteCommand(
    args: Array<String>,
    gitProfileSwitch: GitProfileSwitch,
) {
    if (args.size == 2) {
        val profileName = args[1]
        gitProfileSwitch.deleteProfile(profileName)
    } else {
        println("Usage: profile delete <profileName>")
    }
}

/**
 * Main function to run the GitProfileSwitch application.
 *
 * @param args Command-line arguments.
 */
fun main(args: Array<String>) {
    val gitProfileSwitch = GitProfileSwitch()
    if (args.isEmpty()) {
        gitProfileSwitch.printHelp()
        return
    }

    when (args[0]) {
        "add" -> handleAddCommand(args, gitProfileSwitch)
        "generate" -> gitProfileSwitch.generateProfilesFile()
        "switch" -> gitProfileSwitch.switchProfile()
        "update" -> handleUpdateCommand(args, gitProfileSwitch)
        "delete" -> handleDeleteCommand(args, gitProfileSwitch)
        "view" -> gitProfileSwitch.viewConfigFile()
        "help" -> gitProfileSwitch.printHelp()
        else -> println("Unknown command: ${args[0]}")
    }
}
