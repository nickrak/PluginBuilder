package nickrak.pluginbuilder;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginBuilder extends JavaPlugin
{
	protected Logger l = null;
	protected ConcurrentHashMap<String, String[]> scripts = null;
	protected ConcurrentHashMap<Player, ArrayList<String>> scriptEditors = null;

	public void info(final String msg)
	{
		this.l.info("[PluginBuilder] " + msg);
	}

	@Override
	public void onDisable()
	{
		this.info("Disabled");
		this.l = null;
		this.scripts = null;
		this.scriptEditors = null;
	}

	@Override
	public void onEnable()
	{
		this.l = Logger.getLogger("PluginBuilder");
		this.scripts = new ConcurrentHashMap<String, String[]>();
		this.scriptEditors = new ConcurrentHashMap<Player, ArrayList<String>>();

		final PlayerListener pl = new PlayerListener()
		{
			final PluginBuilder pb = PluginBuilder.this;
			final ConcurrentHashMap<Player, ArrayList<String>> scriptEditors = this.pb.scriptEditors;

			@Override
			public void onPlayerChat(PlayerChatEvent event)
			{
				final Player sender = event.getPlayer();
				String msg = event.getMessage().trim();

				if (event.getMessage().startsWith("!"))
				{
					final String command = msg.split(" ")[0].substring(1);
					final String[] args = msg.substring(command.length()).trim().split(" ");

					if (!this.pb.scripts.containsKey(command))
					{
						return;
					}

					if (this.pb.executeCommand(command, sender, args))
					{
						final StringBuilder sb = new StringBuilder(sender.getName() + " invoked <" + command + "> with these arguments [");
						for (final String arg : args)
						{
							sb.append(arg + ", ");
						}

						String comp = sb.toString();
						comp = comp.substring(0, comp.length() - 2) + "]";
						this.pb.l.info(comp);
					}
					else
					{
						this.pb.l.warning(sender.getName() + " attempted to invoke <" + command + "> which failed.");
					}

					event.setCancelled(true);
				}

				if (this.scriptEditors.containsKey(sender))
				{
					if (msg.startsWith(":"))
					{
						if (msg.equalsIgnoreCase(":h"))
						{
							msg = ":h 1";
						}
						final char command = msg.charAt(1);
						final String data = msg.length() > 2 ? msg.substring(3) : null;
						final ArrayList<String> editor = this.scriptEditors.get(sender);

						switch (command)
						{
						case 'a': // Append
							editor.add(data);
							sender.sendMessage(ChatColor.GRAY + "Appended.");
							break;
						case 's': // Save
							this.pb.scripts.put(data, editor.toArray(new String[0]));
							sender.sendMessage(ChatColor.GRAY + "Saved.");
							break;
						case 'l': // Load
							editor.clear();
							if (!this.pb.scripts.containsKey(data))
							{
								sender.sendMessage(ChatColor.RED + "No such script found.");
								break;
							}
							final String[] lines = this.pb.scripts.get(data);
							for (final String line : lines)
							{
								editor.add(line);
							}
							sender.sendMessage(ChatColor.GRAY + "Loaded.");
							break;
						case 'q': // Quit
							this.scriptEditors.remove(sender);
							sender.sendMessage(ChatColor.GRAY + "Editor Closed.");
							break;
						case 'c': // Clear
							editor.clear();
							sender.sendMessage(ChatColor.GRAY + "Cleared.");
							break;
						case 'p': // Print
							sender.sendMessage(ChatColor.GRAY + "Printing Script Contents...");
							for (int i = 0; i < editor.size(); i++)
							{
								sender.sendMessage(ChatColor.YELLOW + "<" + i + "> " + editor.get(i));
							}
							if (editor.size() == 0)
							{
								sender.sendMessage(ChatColor.YELLOW + "Script is Empty");
							}
							sender.sendMessage(ChatColor.GRAY + "Print Complete.");
							break;
						case 'd': // Delete
							try
							{
								final int line = Integer.parseInt(data);
								editor.remove(line);
								sender.sendMessage(ChatColor.GRAY + "Deleted.");
							}
							catch (final NumberFormatException nfe)
							{
								sender.sendMessage(ChatColor.GRAY + "Not a line number.");
							}
							catch (final IndexOutOfBoundsException be)
							{
								sender.sendMessage(ChatColor.GRAY + "No line with that number.");
							}
							break;
						case 'i': // Insert
							final int space = data.indexOf(' ');
							if (space >= 0)
							{
								try
								{
									final int line = Integer.parseInt(data.substring(0, space));
									editor.add(line, data.substring(space + 1));
								}
								catch (final NumberFormatException nfe)
								{
									sender.sendMessage(ChatColor.GRAY + "Not a line number.");
								}
								catch (final IndexOutOfBoundsException be)
								{
									sender.sendMessage(ChatColor.GRAY + "No line with that number.");
								}
							}
							break;
						case 'h': // Help
						case '?':
						default:
							if (data != null && data.equalsIgnoreCase("2"))
							{
								sender.sendMessage(ChatColor.GRAY + "Usage Information (Page 2)");
								sender.sendMessage(ChatColor.GRAY + ":h [page] // Displays help for the specified page.");
								sender.sendMessage(ChatColor.GRAY + ":l [script name] // Loads a script to the buffer.");
								sender.sendMessage(ChatColor.GRAY + ":s [script name] // Saves the buffer to a script name.");
								sender.sendMessage(ChatColor.GRAY + ":d [line number] // Deletes the specified line.");
								sender.sendMessage(ChatColor.GRAY + ":c // Clears the script editor.");
							}
							else
							{
								sender.sendMessage(ChatColor.GRAY + "Usage Information (Page 1)");
								sender.sendMessage(ChatColor.GRAY + ":h [page] // Displays help for the specified page.");
								sender.sendMessage(ChatColor.GRAY + ":a [code] // Appends the code to the end of the script.");
								sender.sendMessage(ChatColor.GRAY + ":i [line number] [code] // Inserts the code at a specific line.");
								sender.sendMessage(ChatColor.GRAY + ":p // Prints out the contents of the script.");
								sender.sendMessage(ChatColor.GRAY + ":q // Closes the script editor, without saving.");
							}
							break;
						}

						event.setCancelled(true);
					}
				}
			}

			@Override
			public void onPlayerQuit(PlayerQuitEvent event)
			{
				final Player p = event.getPlayer();
				if (this.scriptEditors.containsKey(p))
				{
					this.scriptEditors.remove(p);
				}
			}
		};

		final PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_CHAT, pl, Priority.Highest, this);
		pm.registerEvent(Type.PLAYER_QUIT, pl, Priority.Monitor, this);

	}

	public boolean isValidScript(final String[] parts)
	{
		for (final String line : (parts == null ? new String[0] : parts))
		{
			if (!isValidLine(line))
			{
				return false;
			}
		}
		return true;
	}

	public boolean isValidLine(final String line)
	{
		final String[] cmd = line.split(" ");
		final String[] com = cmd[0].split("::");
		final Plugin plugin = this.getServer().getPluginManager().getPlugin(com[0]);

		if (plugin == null || !(plugin instanceof JavaPlugin))
			return false;

		final JavaPlugin java = (JavaPlugin) plugin;
		final PluginCommand command = java.getCommand(com[1]);

		return command != null;
	}

	public boolean executeCommand(final String scriptName, final Player sender, final String[] args)
	{
		if (!sender.isOp() && !sender.hasPermission("pluginbuilder." + scriptName))
		{
			sender.sendMessage(ChatColor.RED + "I'm sorry Dave, I can't let you do that");
			return true;
		}

		if (this.isValidScript(this.scripts.get(scriptName)))
		{
			final String[] lines = this.scripts.get(scriptName);
			for (final String line : lines)
			{
				try
				{
					this.executeLine(sender, line, args);
				}
				catch (final Exception e)
				{
					if (e.getMessage().equalsIgnoreCase("CANNOT MULTI_NEST SCRIPTS"))
					{
						this.l.warning("Cannot Multi-nest script iterators");
					}
					else
					{
						e.printStackTrace();
					}
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Execute a line of script.
	 * 
	 * @param sender The Player issuing the command
	 * @param line The actual script text defined as
	 *        (<PluginName>::<CommandName>::<Sender> <args>)
	 * @param args Arguments to potentially pass to the script
	 * @throws Exception
	 */
	private void executeLine(final Player sender, final String line, final String[] args) throws Exception
	{
		final PluginManager pm = this.getServer().getPluginManager();
		final String[] cmd = line.split(" ");
		final String[] com = cmd[0].split("::");
		final Plugin p = pm.getPlugin(com[0]);
		final JavaPlugin jp = (p instanceof JavaPlugin ? (JavaPlugin) p : null);

		if (jp == this)
		{
			this.l.warning("Cannot use Plugin Builder recusively");
		}

		int nestedCount = 0;
		if (line.contains("$*"))
		{
			if (line.indexOf("$*") != line.lastIndexOf("$*"))
				nestedCount++;
			nestedCount++;
		}
		if (line.contains("$_"))
		{
			if (line.indexOf("$_") != line.lastIndexOf("$_"))
				nestedCount++;
			nestedCount++;
		}

		if (nestedCount > 1)
		{
			throw new Exception("CANNOT MULTI_NEST SCRIPTS");
		}

		if (jp != null)
		{
			final PluginCommand command = jp.getCommand(com[1]);

			if (command != null)
			{
				final ArrayList<String> argz = new ArrayList<String>();
				for (int i = 1; i < cmd.length; i++)
				{
					String arg = cmd[i];

					for (int a = 0; a < args.length; a++)
					{
						arg = arg.replace("$" + a, args[a]);
					}

					arg = arg.replace("$!", sender.getName());
					argz.add(arg);
				}

				String imp = com[2];
				if (imp.equals("$!"))
				{
					imp = sender.getName();
				}
				for (int a = 0; a < args.length; a++)
				{
					imp = imp.replace("$" + a, args[a]);
				}

				if (!imp.contains("$*") && !imp.contains("$_") && !imp.contains("$@"))
				{
					final Player player = this.getServer().getPlayer(imp);
					command.execute(player, com[1], argz.toArray(new String[0]));
				}
				else
				{
					if (imp.equals("$*"))
					{
						for (final Player player : this.getServer().getOnlinePlayers())
						{
							command.execute(player, com[1], argz.toArray(new String[0]));
						}
					}
					if (imp.startsWith("$_"))
					{
						for (final Player player : this.getServer().getOnlinePlayers())
						{
							if (imp.equals("$_") && player.isOp())
							{
								command.execute(player, com[1], argz.toArray(new String[0]));
							}
							if (!imp.equals("$_") && player.hasPermission(imp.substring(2)))
							{
								command.execute(player, com[1], argz.toArray(new String[0]));
							}
						}
					}
					if (imp.startsWith("$@"))
					{
						for (final Player player : this.getServer().getWorld(imp.substring(2)).getPlayers())
						{
							command.execute(player, com[1], argz.toArray(new String[0]));
						}
					}
				}
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (sender instanceof Player)
		{
			final Player player = (Player) sender;
			if (command.getName().equalsIgnoreCase("pb"))
			{
				if (this.scriptEditors.containsKey(player))
				{
					player.sendMessage(ChatColor.RED + "You already have an editor open.");
				}
				else
				{
					player.sendMessage(ChatColor.YELLOW + "You have opened the script editor");
					this.scriptEditors.put(player, new ArrayList<String>());
				}
				return true;
			}

		}
		return false;
	}
}
