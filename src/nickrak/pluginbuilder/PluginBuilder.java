package nickrak.pluginbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
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
import org.bukkit.util.config.Configuration;

public final class PluginBuilder extends JavaPlugin
{
	protected Logger l = null;
	protected ConcurrentHashMap<String, String[]> scripts = null;
	protected ScriptEditor scriptEditor;

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
		this.scriptEditor = null;
	}

	protected void saveScripts()
	{
		final Configuration config = this.getConfiguration();
		config.load();

		final ArrayList<String> scripts = new ArrayList<String>();
		config.removeProperty("source");

		for (final String scriptName : this.scripts.keySet())
		{
			final ArrayList<String> scriptContents = new ArrayList<String>();
			for (final String line : this.scripts.get(scriptName))
			{
				scriptContents.add(line);
			}
			if (scriptContents.size() > 0)
			{
				config.setProperty("source." + scriptName, scriptContents);
				scripts.add(scriptName);
			}
		}

		config.setProperty("scripts", scripts);
		config.save();
	}

	protected void loadScripts()
	{
		final Configuration config = this.getConfiguration();
		config.load();

		final List<String> scripts = config.getStringList("scripts", new ArrayList<String>());
		for (final String scriptName : scripts)
		{
			final List<String> scriptContents = config.getStringList("source." + scriptName, null);
			if (scriptContents != null && !scriptContents.isEmpty())
			{
				this.scripts.put(scriptName, scriptContents.toArray(new String[0]));
			}
		}
	}

	@Override
	public void onEnable()
	{
		this.l = Logger.getLogger("PluginBuilder");
		this.scripts = new ConcurrentHashMap<String, String[]>();

		this.loadScripts();

		final PlayerListener pl = new PlayerListener()
		{
			final PluginBuilder pb = PluginBuilder.this;

			@Override
			public void onPlayerChat(PlayerChatEvent event)
			{
				final Player sender = event.getPlayer();
				String msg = event.getMessage().trim();

				if (event.getMessage().startsWith("!"))
				{
					final String command = msg.split(" ")[0].substring(1);
					final String[] args = msg.substring(command.length()).trim().split(" ");

					if (!this.pb.scripts.containsKey(command.toLowerCase()))
					{
						return;
					}

					if (this.pb.executeCommand(command.toLowerCase(), sender, args))
					{
						final StringBuilder sb = new StringBuilder(sender.getName() + " invoked <" + command.toLowerCase()
								+ "> with these arguments [");
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
						this.pb.l.warning(sender.getName() + " attempted to invoke <" + command.toLowerCase() + "> which failed.");
					}

					event.setCancelled(true);
				}
			}

			@Override
			public void onPlayerQuit(PlayerQuitEvent event)
			{
				this.pb.scriptEditor.playerQuit(event.getPlayer());
			}
		};

		final PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Type.PLAYER_CHAT, pl, Priority.Highest, this);
		pm.registerEvent(Type.PLAYER_QUIT, pl, Priority.Monitor, this);

		this.scriptEditor = new ScriptEditor(this);
		this.getCommand("pb").setExecutor(this.scriptEditor);

		this.info("Enabled <" + this.getDescription().getVersion() + ">");
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
						this.l.warning(e.getMessage());
						e.printStackTrace();
						sender.sendMessage(e.getMessage());
					}
					return false;
				}
			}
			return true;
		}
		sender.sendMessage(ChatColor.RED + "Not a valid script: <" + scriptName + ">");
		this.l.warning("Not a valid script: <" + scriptName + ">");
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
}
