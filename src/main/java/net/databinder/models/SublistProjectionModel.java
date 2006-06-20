package net.databinder.models;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import wicket.Component;
import wicket.WicketRuntimeException;
import wicket.model.AbstractDetachableModel;
import wicket.model.IModel;

/**
 * Projects a single list into a multiple, arbitrarily transformed sublists without replicating
 *  the list data. A parent list containing sublists is the object wrapped in this model,
 *  while the master list model passed in is held and used internally.
 *  
 * @author Nathan Hamblen
 */
public abstract class SublistProjectionModel extends AbstractDetachableModel
    {
        /** Continuous list used to feed this model's sublists. */
        private IModel master;

        public SublistProjectionModel(IModel master)
        {
            this.master = master;
        }
        
        /** @return number of sublists */
        protected abstract int getParentSize();
        
        /** @return index of master list mapped to parameters */
        protected abstract int transform(int parentIdx, int sublistIdx);
        
        /** @return size sublist at given index*/
        protected abstract int getSize(int parentIdx);
        
        /**
         * Breaks the parent list into chunks of the requested size, in the same order as
         * the parent list.
         */
		public static class Chunked extends SublistProjectionModel {
		    
		    protected int chunkSize;
		    
		    public Chunked(int chunkSize, IModel master)
		    {
		        super(master);
		        this.chunkSize = chunkSize;
		    }
		
		    protected int transform(int parentIdx, int sublistIdx)
		    {
		        return parentIdx * chunkSize + sublistIdx;
		    }
		
		    protected int getSize(int parentIdx) {
		    	return Math.min(getMasterList().size() - parentIdx * chunkSize, chunkSize);
		    }
		
		    protected int getParentSize()
		    {
		        return (getMasterList().size() - 1) / chunkSize + 1;
		    }
		}

        /**
         * Transposes rows and columns so the list runs top to bottom rather than
         * left to right. 
         */
        public static class Transposed extends Chunked  {
            
            public Transposed(int columns, IModel master)
            {
                super(columns, master);
            }

            protected int transform(int parentIdx, int sublistIdx)
            {
                return parentIdx + sublistIdx * getParentSize();
            }
        }

        protected List getMasterList()
        {
            return (List)master.getObject(null);
        }
        
        /**
         * Instantiates parent list, made up entirely of projected sublists.
         */
        @Override
        protected Object onGetObject(Component component)
        {
            int rows = getParentSize();
            List<List> parent = new ArrayList<List>(rows);
            for (int i = 0; i < rows; i++)
                parent.add(new ProjectedSublist(i));
            return parent;
        }

        @Override
        public void onSetObject(Component component, Object object)
        {
            throw new WicketRuntimeException("This model is read only.");
        }

        /**
         * This is a virtual list, a projection of the master list. Its size and index trasform is 
         * governed by the containing object.
         */
        @SuppressWarnings("unchecked")
        protected class ProjectedSublist extends AbstractList
        {
            private int parentIdx;

            public ProjectedSublist(final int parentIdx)
            {
                this.parentIdx = parentIdx;
            }

            @Override
            public Object get(final int index)
            {
                return getMasterList().get(transform(parentIdx, index));
            }

            @Override
            public int size()
            {
                return getSize(parentIdx);
            }
        }

		@Override
        public void onDetach()
        {
            master.detach();
        }

        @Override
        public IModel getNestedModel()
        {
            return null;
        }

        @Override
        protected void onAttach()
        {
        }
}