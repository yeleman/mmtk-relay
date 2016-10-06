package com.yeleman.mmtkrelay;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

public class OperationAdapter extends ArrayAdapter<Operation> {

    private static class ViewHolder {
        TextView tvAction;
        TextView tvCreatedOn;
        TextView tvAmountOrText;
        TextView tvMsisdn;
        TextView tvTransactionId;
    }

    public OperationAdapter(Context context, ArrayList<Operation> users) {
        super(context, R.layout.item_operation, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Operation operation = getItem(position);

        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_operation, parent, false);
            viewHolder.tvAction = (TextView) convertView.findViewById(R.id.tvAction);
            viewHolder.tvCreatedOn = (TextView) convertView.findViewById(R.id.tvCreatedOn);
            viewHolder.tvAmountOrText = (TextView) convertView.findViewById(R.id.tvAmountOrText);
            viewHolder.tvMsisdn = (TextView) convertView.findViewById(R.id.tvMsisdn);
            viewHolder.tvTransactionId = (TextView) convertView.findViewById(R.id.tvTransactionId);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Populate the data into the template view using the data object
        viewHolder.tvAction.setText(operation.getLabel());
        viewHolder.tvAction.setBackgroundColor(Constants.getStatusColor(operation.getBooleanStatus()));
        viewHolder.tvCreatedOn.setText(operation.getFormattedCreatedOn());
        if (operation.isTransaction() || operation.isBalance()) {
            viewHolder.tvAmountOrText.setText(operation.getFormattedAmountAndFees());
        } else {
            viewHolder.tvAmountOrText.setText(operation.getStrippedText());
        }
        viewHolder.tvMsisdn.setText(operation.getFormattedMsisdn());
        viewHolder.tvTransactionId.setText(operation.getStrippedTransactionId());

        return convertView;
    }
}