package thredds.server.catalogservice;

import java.beans.PropertyEditorSupport;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CommandEditor extends PropertyEditorSupport
{
  public CommandEditor()
  {
    super();
  }

  public String getAsText()
  {
    Command c = (Command) super.getValue();
    return c.toString();
  }

  public void setAsText( String text ) throws IllegalArgumentException
  {
    if ( text == null || text.equals( "" ) )
    {
      super.setValue( Command.SHOW );
      return;
    }
    Command c = Command.valueOf( text.toUpperCase() );
    super.setValue( c );
  }
}