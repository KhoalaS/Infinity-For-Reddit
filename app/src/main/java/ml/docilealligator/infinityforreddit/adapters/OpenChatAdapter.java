package ml.docilealligator.infinityforreddit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ml.docilealligator.infinityforreddit.R;

public class OpenChatAdapter  extends RecyclerView.Adapter<OpenChatAdapter.ViewHolder> {
    private JSONArray localDataSet;
    private String username;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView chatNameTextView;
        private final TextView lastMessageTextView;
        private final TextView lastMessageDateTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            chatNameTextView = view.findViewById(R.id.item_open_chat_name);
            lastMessageTextView = view.findViewById(R.id.item_open_chat_lastmessage);
            lastMessageDateTextView = view.findViewById(R.id.item_open_chat_lastdate);

        }

        public TextView getChatNameTextView() {
            return chatNameTextView;
        }

        public TextView getLastMessageTextView() {
            return lastMessageTextView;
        }

        public TextView getLastMessageDateTextView() {
            return lastMessageDateTextView;
        }
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     */
    public OpenChatAdapter(JSONArray dataSet, String username) {
        localDataSet = dataSet;
        this.username = username;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_open_chat, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        try {
            JSONObject node = localDataSet.getJSONObject(position).getJSONObject("node");
            String lastMesageDate = node
                    .getJSONObject("lastMessage")
                    .getString("createdAt");

            int memberCount = node.getInt("memberCount");
            String chatName = "";
            JSONArray channelMembers = node.getJSONArray("channelMembers");
            for(int i = 0; i < memberCount; i++){
                String name = channelMembers.getJSONObject(i).getJSONObject("redditor").getString("name");
                if(!name.equals(username)){
                    chatName = name;
                    break;
                }
            }

            String split[] = lastMesageDate.split("T");


            JSONObject lastMessage = node.getJSONObject("lastMessage");
            String message = lastMessage.getString("message");
            String sender = lastMessage.getJSONObject("sender").getString("name");
            String prefix = sender.equals(username) ? "You" : sender;
            String url;

            String customType = lastMessage.getString("customType");
            if(customType.equals("IMAGE")){
                url = message;
                message = " sent an image";
            }else{
                prefix += ": ";
            }
            viewHolder.getLastMessageDateTextView().setText(split[0]);
            viewHolder.getChatNameTextView().setText(chatName);
            viewHolder.getLastMessageTextView().setText(prefix + message);


        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.length();
    }
}



