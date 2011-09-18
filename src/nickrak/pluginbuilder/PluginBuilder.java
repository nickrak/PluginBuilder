package nickrak.pluginbuilder;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginBuilder extends JavaPlugin
{
	private Logger l = null;
	private ConcurrentHashMap<String, String[]> scripts = null;

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
	}

	@Override
	public void onEnable()
	{
		this.l = Logger.getLogger("PluginBuilder");
		this.scripts = new ConcurrentHashMap<String, String[]>();
	}

	public boolean isValidScript(String[] parts)
	{
		return true;
	}

	public boolean executeCommand(String scriptName, Player sender, String[] args)
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
	private void executeLine(Player sender, String line, String[] args) throws Exception
	{
		final PluginManager pm = this.getServer().getPluginManager();
		final String[] cmd = line.split(" ");
		final String[] com = cmd[0].split("::");
		final Plugin p = pm.getPlugin(com[0]);
		final JavaPlugin jp = (p instanceof JavaPlugin ? (JavaPlugin) p : null);

		if (jp == this)
		{
			this.l.warning("Cannot use Plugin Builder recusively");
			return;
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
		return super.onCommand(sender, command, label, args);
	}
}
