package ibanez.jacob.cat.xtec.ioc.lectorrss;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ibanez.jacob.cat.xtec.ioc.lectorrss.model.RssItem;

/**
 * @author <a href="mailto:jacobibanez@jacobibanez.com">Jacob Ibáñez Sánchez</a>.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemAdapterViewHolder> {

    private List<RssItem> mItems;
    private Context mContext;

    public ItemAdapter(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * This is handy if we want to add new items to the list, but don't want to instantiate a new
     * adapter.
     *
     * @param items The new list
     */
    public void setItems(List<RssItem> items) {
        this.mItems = items;
        this.notifyDataSetChanged();
    }

    /**
     * Creates a new row for the Recycler View
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View
     * @return A new ViewHolder that holds a View of the given view type
     */
    @Override
    public ItemAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rss_item, parent, false);
        return new ItemAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemAdapterViewHolder holder, int position) {
        RssItem item = mItems.get(position);

        //we set the content of the item view
        boolean hasCachePath = item.getImagePathInCache() != null && item.getImagePathInCache().length() > 0;
        boolean pathExists = Drawable.createFromPath(item.getImagePathInCache()) != null;
        if (hasCachePath && pathExists) {
            holder.mThumbnail.setImageDrawable(Drawable.createFromPath(item.getImagePathInCache()));
        } else {
            holder.mThumbnail.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        holder.mTitle.setText(item.getTitle());
    }

    @Override
    public int getItemCount() {
        return mItems != null && !mItems.isEmpty() ? mItems.size() : 0;
    }

    /**
     * This is the class for representing a single item in the recycler view. It also implements
     * {@link View.OnClickListener} and {@link View.OnLongClickListener}
     * <p>
     * When a regular click is done, a new {@link RssItemActivity} is created. If a long click is
     * performed, the item from the list is removed.
     */
    public class ItemAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        public final ImageView mThumbnail;
        public final TextView mTitle;

        public ItemAdapterViewHolder(View itemView) {
            super(itemView);

            //Get references to the item's views
            mThumbnail = itemView.findViewById(R.id.item_thumbnail);
            mTitle = itemView.findViewById(R.id.item_title);

            //set click listener and long click listener
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //we get the position of the clicked view holder and get the selected item
            int position = getAdapterPosition();
            RssItem item = mItems.get(position);

            //create an intent to open the selected item in a new activity
            Intent intent = new Intent(mContext, RssItemActivity.class);

            //put the item in a bundle
            Bundle extras = new Bundle();
            extras.putSerializable(RssItemActivity.EXTRA_ITEM, item);

            //put the bundle in the intent and start the new activity
            intent.putExtras(extras);
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(intent);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            //we get the position of the clicked view holder and remove the selected item from the list
            int position = getAdapterPosition();
            mItems.remove(position);
            notifyItemRemoved(position);
            return true;
        }
    }
}
