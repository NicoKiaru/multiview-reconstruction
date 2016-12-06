package spim.fiji.spimdata.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import mpicbg.models.ElasticMovingLeastSquaresMesh;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.Tile;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import spim.fiji.spimdata.SpimDataTools;
import spim.fiji.spimdata.explorer.ExplorerWindow;


public class FilteredAndGroupedTableModel < AS extends AbstractSpimData< ? > > extends AbstractTableModel implements ISpimDataTableModel<AS>
{
	
	
	private static final long serialVersionUID = -6526338840427674269L;

	protected List< List< BasicViewDescription< ? > >> elements = null;
	
	final ExplorerWindow< AS, ? > panel;
	Set<Class<? extends Entity>> groupingFactors;
	Map<Class<? extends Entity>, List<? extends Entity>> filters;
	List<Class<? extends Entity>> columnClasses;
	List<Class<? extends Entity>> sortingFactors;
	
	
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#getPanel()
	 */
	@Override
	public ExplorerWindow< AS, ? > getPanel() {
		return panel;
	}
	
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#clearSortingFactors()
	 */
	@Override
	public void clearSortingFactors() {
		sortingFactors.clear();
	}
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#addSortingFactor(java.lang.Class)
	 */
	@Override
	public void addSortingFactor(Class<? extends Entity> factor){
		if(sortingFactors.contains(factor))
			sortingFactors.remove(factor);
		sortingFactors.add(factor);
	}

	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#clearGroupingFactors()
	 */
	@Override
	public void clearGroupingFactors() {
		groupingFactors.clear();
	}
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#addGroupingFactor(java.lang.Class)
	 */
	@Override
	public void addGroupingFactor(Class<? extends Entity> factor ) {
		groupingFactors.add(factor);
		fireTableDataChanged();
	}
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#clearFilters()
	 */
	@Override
	public void clearFilters() {
		filters.clear();	
	}
	
	/* (non-Javadoc)
	 * @see gui.ISpimDataTableModel#addFilter(java.lang.Class, java.util.List)
	 */
	@Override
	public void addFilter(Class<? extends Entity> cl, List<? extends Entity> instances){
		filters.put(cl, instances);
		fireTableDataChanged();
	}
	
	public static ArrayList<Class<? extends Entity>> defaultColumnClassesStitching()
	{
		ArrayList<Class <? extends Entity>> res = new ArrayList<>();
		res.add(TimePoint.class);
		res.add(ViewSetup.class);
		//res.add(Angle.class);
		res.add(Channel.class);
		res.add(Illumination.class);
		res.add(Tile.class);		
		return res;
	}
	
	public static ArrayList<Class<? extends Entity>> defaultColumnClassesMV()
	{
		ArrayList<Class <? extends Entity>> res = new ArrayList<>();
		res.add(TimePoint.class);
		res.add(ViewSetup.class);
		res.add(Illumination.class);
		res.add(Channel.class);
		res.add(Angle.class);		
		res.add(Tile.class);		
		return res;
	}
	
	
	
	public FilteredAndGroupedTableModel( final ExplorerWindow< AS, ? > panel )
	{
		groupingFactors = new HashSet<>();
		filters = new HashMap<>();
		this.panel = panel;
		columnClasses = defaultColumnClassesStitching();
		
		sortingFactors = new ArrayList<>();
		// default: sort by ViewSetup, then by TimePoint
		sortingFactors.add(ViewSetup.class);
		sortingFactors.add(TimePoint.class);
				
		elements();
	}
	
	

	protected List<List< BasicViewDescription< ? extends BasicViewSetup > >> elements()
	{
		final List<BasicViewDescription< ? > > ungroupedElements =
				SpimDataTools.getFilteredViewDescriptions( panel.getSpimData().getSequenceDescription(), filters);
		final List< List< BasicViewDescription< ?  > >> elementsNew = 
				SpimDataTools.groupByAttributes(ungroupedElements, groupingFactors);

		
		// sort the grouped VDs
		for (List< BasicViewDescription< ?  > > l : elementsNew)
		{
			for (Class<? extends Entity> cl : sortingFactors)
				Collections.sort(l, SpimDataTools.getVDComparator(cl));
		}
		
		// sort the groups of VDS
		for (Class<? extends Entity> cl : sortingFactors)
			Collections.sort(elementsNew, SpimDataTools.getVDListComparator(cl));
		
		//if ( this.elements == null )
			this.elements = elementsNew;

		return elements;
	}

	/**
	 * clicking on a column adds that columns class as the last class to sort by
	 * @param column
	 */
	@Override
	public void sortByColumn( final int column )
	{
		addSortingFactor(columnClasses.get(column));
		elements();
		fireTableDataChanged();
	}

	@Override
	public List<List< BasicViewDescription< ?  >> > getElements() { return elements(); }

	@Override
	public int getColumnCount() { return columnClasses.size(); }
	
	@Override
	public int getRowCount() { return elements().size(); }

	@Override
	public boolean isCellEditable( final int row, final int column )
	{
		return false;
	}

	@Override
	public Object getValueAt( final int row, final int column )
	{
		final List<BasicViewDescription< ? extends BasicViewSetup >> vds = elements().get( row );

		Class <? extends Entity> c = columnClasses.get(column);
		final HashSet<Entity> entries = new HashSet<>();
		
		for (BasicViewDescription< ? extends BasicViewSetup > vd : vds)
		{
			if ( c == TimePoint.class )
				entries.add(vd.getTimePoint());
			else if ( c == ViewSetup.class )
				entries.add(vd.getViewSetup());
			else
			{
				for (Entity ei : vd.getViewSetup().getAttributes().values()){
					if (c.isInstance(ei)){
						entries.add(ei);
					}
				}
			}
		}
		List<Entity> sorted = new ArrayList<>(entries);
		Entity.sortById(sorted);

		
		final ArrayList<String> entryNames = new ArrayList<>();
		
		if (entries.size() < 1)
			return "";
		else
		{
			for (final Entity e : sorted)
			{
				if (e instanceof NamedEntity)
					entryNames.add(((NamedEntity) e).getName() + "(id=" + e.getId() + ")");
				else
					entryNames.add(Integer.toString(e.getId()));
			}
		}
		
		return String.join(", ", entryNames);
		
		
	}

	@Override
	public String getColumnName( final int column )
	{
		return columnClasses.get( column ).getSimpleName();
	}


	public void setColumnClasses(List< Class< ? extends Entity > > columnClasses)
	{
		this.columnClasses = columnClasses;
	}


	@Override
	public int getSpecialColumn(ISpimDataTableModel.SpecialColumnType type)
	{
		return -1;
	}


	@Override
	public Set< Class< ? extends Entity > > getGroupingFactors()
	{
		return groupingFactors;
	}
}
