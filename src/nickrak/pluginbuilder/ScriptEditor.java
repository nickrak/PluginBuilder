package nickrak.pluginbuilder;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScriptEditor implements CommandExecutor
{
	private final PluginBuilder pb;
	private final ConcurrentHashMap<String, String[]> scripts;
	private final ConcurrentHashMap<CommandSender, ArrayList<String>> scriptEditors;

	public ScriptEditor(final PluginBuilder pb)
	{
		this.pb = pb;
		this.scripts = pb.scripts;
		this.scriptEditors = new ConcurrentHashMap<CommandSender, ArrayList<String>>();
	}

	public void playerQuit(final Player p)
	{
		this.scriptEditors.remove(p);
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3)
	{
		if (arg1.getName().equalsIgnoreCase("pb"))
		{
			return this.com(arg0, arg3);
		}
		return false;
	}

	private boolean com(final CommandSender sender, final String[] args)
	{
		if (!this.scriptEditors.containsKey(sender))
		{
			this.scriptEditors.put(sender, new ArrayList<String>());
		}

		final ArrayList<String> thisEditor = this.scriptEditors.get(sender);

		if (args == null || args.length == 0)
		{
			this.displayHelp(sender);
			return true;
		}
		else if (args.length == 1)
		{
			final String arg = args[0].toLowerCase();
			if (arg.equals("") || arg.equals("h") || arg.equals("help"))
			{
				this.displayHelp(sender);
				return true;
			}
			else if (arg.equals("p") || arg.equals("print"))
			{
				if (thisEditor.size() > 0)
				{
					sender.sendMessage(ChatColor.YELLOW + "Script Contents:");
					for (int i = 0; i < thisEditor.size(); i++)
					{
						sender.sendMessage(ChatColor.YELLOW + "<" + i + "> " + ChatColor.AQUA + thisEditor.get(i));
					}
					sender.sendMessage(ChatColor.YELLOW + "End of Contents");
				}
				else
				{
					sender.sendMessage(ChatColor.YELLOW + "Script is Empty");
				}
				return true;
			}
			else if (arg.equals("c") || arg.equals("clear") || arg.equals("erase"))
			{
				thisEditor.clear();
				sender.sendMessage(String.format("%s<*> << Cleared.", ChatColor.YELLOW));
				return true;
			}
			else if (arg.equals("ls") || arg.equals("list"))
			{
				final ArrayList<String> scriptNames = new ArrayList<String>(this.scripts.keySet());
				if (scriptNames.size() > 0)
				{
					sender.sendMessage(ChatColor.YELLOW + "All Available Scripts:");
					for (int i = 0; i < scriptNames.size(); i++)
					{
						sender.sendMessage(ChatColor.AQUA + scriptNames.get(i));
					}
				}
				else
				{
					sender.sendMessage(ChatColor.YELLOW + "No Scripts Available");
				}
				return true;
			}

			if (args[0].startsWith("?"))
			{
				final String c = args[0].toLowerCase().substring(1);
				if (c.equals("s") || c.equals("save"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [s|save] <scriptname>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Saves the given script under the specified name."));
				}
				else if (c.equals("l") || c.equals("load"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [l|load] <scriptname>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Loads a pre-existing script into the editor."));
				}
				else if (c.equals("a") || c.equals("append") || c.equals("add"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [a|add|append] <code>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Appends the given line to the end of the editor."));
				}
				else if (c.equals("i") || c.equals("insert"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [i|insert] <line> <code>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Inserts the given line in the editor at the specified line."));
				}
				else if (c.equals("d") || c.equals("delete"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [d|delete] <line>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Deletes the specified line."));
				}
				else if (c.equals("r") || c.equals("replace"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [r|replace] <line> <code>"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Replaces the specified line with the given code."));
				}
				else if (c.equals("ls") || c.equals("list"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [ls|list]"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Display all available scripts."));
				}
				else if (c.equals("c") || c.equals("clear") || c.equals("erase"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [c|clear|erase]"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Clears the contents of the editor."));
				}
				else if (c.equals("p") || c.equals("print"))
				{
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "usage: /pb [p|print]"));
					sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, "Display the contents of the editor."));
				}
				else if (c.equals("language"))
				{
					sender.sendMessage(ChatColor.YELLOW + "Syntax: <pluginName>::<commandName>::<caller> <arguments>");
					sender.sendMessage(ChatColor.YELLOW + "Macros: $*, $!, $_, $@");
					sender.sendMessage(ChatColor.YELLOW + "Use /pb ?language:<macro or placeholder> for more info");
				}
				else if (c.equals("language:pluginname"))
				{
					sender.sendMessage(ChatColor.AQUA + "<pluginName>" + ChatColor.YELLOW + " should have a the plugins internal name.");
					sender.sendMessage(ChatColor.YELLOW + "If you don't know your target plugin's name, use /plugins.");
					sender.sendMessage(ChatColor.YELLOW + "Or, check the target plugin's plugin.yml file.");
				}
				else if (c.equals("language:commandname"))
				{
					sender.sendMessage(ChatColor.AQUA + "<commandName>" + ChatColor.YELLOW + " should have a the plugins internal command name.");
					sender.sendMessage(ChatColor.YELLOW + "If you don't know your target plugin's name, try the /<> text.");
					sender.sendMessage(ChatColor.YELLOW + "Or, check the target plugin's plugin.yml file.");
				}
				else if (c.equals("language:caller"))
				{
					sender.sendMessage(ChatColor.AQUA + "<caller>" + ChatColor.YELLOW + " should be the player you want to come from.");
					sender.sendMessage(ChatColor.YELLOW + "Most of the time, you should use the $! macro here.");
					sender.sendMessage(ChatColor.YELLOW + "Putting a players name here will cause the script to fail, ");
					sender.sendMessage(ChatColor.YELLOW + "if that player is not logged in.");
				}
				else if (c.equals("language:arguments"))
				{
					sender.sendMessage(ChatColor.AQUA + "<arguments>" + ChatColor.YELLOW + " should be whatever text comes after the command name.");
					sender.sendMessage(ChatColor.YELLOW + "You can hard code information here, or use macros.");
					sender.sendMessage(ChatColor.YELLOW + "Arguments passed to the script will be available as $0-$n.");
				}
				else if (c.equals("language:$*"))
				{
					sender.sendMessage(ChatColor.AQUA + "$*" + ChatColor.YELLOW + " is a macro for everyone on the server.");
					sender.sendMessage(ChatColor.YELLOW + "This is a nesting macro, you may only have one nesting macro per line.");
				}
				else if (c.equals("language:$!"))
				{
					sender.sendMessage(ChatColor.AQUA + "$!" + ChatColor.YELLOW + " is a macro for the Player calling the script.");
				}
				else if (c.equals("language:$_"))
				{
					sender.sendMessage(ChatColor.AQUA + "$_<permissionNode>" + ChatColor.YELLOW
							+ " is a macro for everyone who has the specified permission node.");
					sender.sendMessage(ChatColor.YELLOW + "This is a nesting macro, you may only have one nesting macro per line.");
				}
				else if (c.equals("language:$@"))
				{
					sender.sendMessage(ChatColor.AQUA + "$@<worldName>" + ChatColor.YELLOW + " is a macro for everyone in the specified world.");
					sender.sendMessage(ChatColor.YELLOW + "This is a nesting macro, you may only have one nesting macro per line.");
				}
				else
				{
					return false;
				}
				return true;
			}
			return false;
		}
		else
		{

			final String c = args[0].toLowerCase();
			if (c.equals("s") || c.equals("save"))
			{
				final String saveName = args[1].toLowerCase();
				if (saveName.equals("build"))
				{
					sender.sendMessage(ChatColor.RED + "Cannot save as 'build', that name is reserved.");
				}
				else
				{
					this.scripts.put(saveName, thisEditor.toArray(new String[0]));
					this.pb.saveScripts();
					sender.sendMessage(ChatColor.YELLOW + "Saved.");
				}
				return true;
			}
			else if (c.equals("l") || c.equals("load"))
			{
				final String saveName = args[1].toLowerCase();
				if (saveName.equals("build"))
				{
					sender.sendMessage(ChatColor.RED + "Cannot load 'build', that name is reserved.");
				}
				else
				{
					final String[] s = this.scripts.get(saveName);
					thisEditor.clear();
					for (final String l : s)
					{
						thisEditor.add(l);
					}
					sender.sendMessage(ChatColor.YELLOW + "Loaded.");
				}
				return true;
			}
			else if (c.equals("a") || c.equals("append") || c.equals("add"))
			{
				final StringBuilder commandText = new StringBuilder();
				for (int i = 1; i < args.length; i++)
				{
					commandText.append(args[i] + " ");
				}
				final String co = commandText.toString();
				thisEditor.add(co.substring(0, co.length() - 1));
				sender.sendMessage(String.format("%s<%d> << %s", ChatColor.YELLOW, (thisEditor.size() - 1), co.substring(0, co.length() - 1)));
				return true;
			}
			else if (c.equals("i") || c.equals("insert"))
			{
				final StringBuilder commandText = new StringBuilder();
				for (int i = 2; i < args.length; i++)
				{
					commandText.append(args[i] + " ");
				}
				final String co = commandText.toString();
				final int line = Integer.parseInt(args[1]);
				thisEditor.add(line, co.substring(0, co.length() - 1));
				sender.sendMessage(String.format("%s<%d> << %s", ChatColor.YELLOW, line, co.substring(0, co.length() - 1)));
				return true;
			}
			else if (c.equals("d") || c.equals("delete"))
			{
				final int line = Integer.parseInt(args[1]);
				thisEditor.remove(line);
				sender.sendMessage(String.format("%s<%d> --", ChatColor.YELLOW, line));
				return true;
			}
			else if (c.equals("r") || c.equals("replace"))
			{
				final StringBuilder commandText = new StringBuilder();
				for (int i = 2; i < args.length; i++)
				{
					commandText.append(args[i] + " ");
				}
				final String co = commandText.toString();
				final int line = Integer.parseInt(args[1]);
				thisEditor.set(line, co.substring(0, co.length() - 1));
				sender.sendMessage(String.format("%s<%d> << %s", ChatColor.YELLOW, line, co.substring(0, co.length() - 1)));
				return true;
			}
			return false;
		}
	}

	private void displayHelp(final CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "Plugin Builder -- Version: " + ChatColor.AQUA + this.pb.getDescription().getVersion());
		sender.sendMessage("usage: /pb [command]");
		sender.sendMessage("usage: /pb ?[command]   " + ChatColor.GRAY + "# View Help for specified command.");
		sender.sendMessage("usage: /pb ?language    " + ChatColor.GRAY + "# View script language help.");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.YELLOW + "Command List:");
		sender.sendMessage(ChatColor.YELLOW + "Editing-> " + ChatColor.WHITE + "append, insert, delete, replace, clear, print");
		sender.sendMessage(ChatColor.YELLOW + "Management-> " + ChatColor.WHITE + "save, load, list");
	}
}
